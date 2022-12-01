#!/bin/bash
set -e
# set -euxo pipefail

# Switch to Aiven project
avn project switch avaya-d337

# T-5AHCQ - Kafka Streams restore consumer DNS issue on Aiven Kafka cluster rolling upgrades
SERVICE_NAME="kstreams-issue"

# Create Kafka service
avn service create \
    --service-type kafka \
    --cloud azure-westeurope \
    --plan startup-2 \
    --no-fail-if-exists \
    ${SERVICE_NAME}

# Wait for Kafka service to reach the RUNNING state
avn service wait ${SERVICE_NAME}

# Configure Kafka Service
avn service update ${SERVICE_NAME} \
    -c kafka_authentication_methods.sasl=true \
    -c schema_registry=true \
    -c public_access.kafka=true \
    -c public_access.schema_registry=true

# Get CA and add to Truststore
avn service user-creds-download --username avnadmin -d ./creds ${SERVICE_NAME}
# sudo keytool -delete -trustcacerts -cacerts -storepass changeit -noprompt -alias aiven
# sudo keytool -importcert -v -trustcacerts -cacerts -storepass changeit -noprompt -alias aiven -file ./creds/ca.pem

# Get credentials and update application.yaml of apps below
SVC_HOST=$( avn service get ${SERVICE_NAME} --json | jq -r '.components[] | select(.route=="public" and .component=="kafka" and .kafka_authentication_method=="sasl").host' )
SVC_PORT=$( avn service get ${SERVICE_NAME} --json | jq -r '.components[] | select(.route=="public" and .component=="kafka" and .kafka_authentication_method=="sasl").port' )

SVC_USERNAME=$( avn service user-get ${SERVICE_NAME} --username avnadmin --json | jq -r .username )
SVC_PASSWORD=$( avn service user-get ${SERVICE_NAME} --username avnadmin --json | jq -r .password )

SR_HOST=$( avn service get ${SERVICE_NAME} --json | jq -r '.components[] | select(.route=="public" and .component=="schema_registry").host' )
SR_PORT=$( avn service get ${SERVICE_NAME} --json | jq -r '.components[] | select(.route=="public" and .component=="schema_registry").port' )

printf "\n\nAdd the following to the SpringBoot application.yaml\n\n"

printf "kafka:\n"
printf "  serviceUri: %s:%s\n" ${SVC_HOST} ${SVC_PORT}
printf "  username: %s\n" ${SVC_USERNAME}
printf "  password: %s\n" ${SVC_PASSWORD}
printf "  schemaRegistryUri: https://%s:%s@%s:%s\n" ${SVC_USERNAME} ${SVC_PASSWORD} ${SR_HOST} ${SR_PORT}

printf "\n\n"

# Create input-topic
# avn service topic-delete ${SERVICE_NAME} input-topic
avn service topic-create ${SERVICE_NAME} input-topic \
    --partitions 5 \
    --replication 2 \
    --retention 2 \
    --cleanup-policy compact

# Create output-topic
# avn service topic-delete ${SERVICE_NAME} output-topic
avn service topic-create ${SERVICE_NAME} output-topic \
    --partitions 5 \
    --replication 2 \
    --retention 2 \
    --cleanup-policy compact