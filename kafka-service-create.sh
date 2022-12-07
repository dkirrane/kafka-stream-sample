#!/bin/bash
set -e
# set -euxo pipefail

set -a; source .env; set +a
SCRIPT_PATH="$(dirname -- "${BASH_SOURCE[0]}")"

# Cleanup
rm -Rf ${SCRIPT_PATH}/logs
rm -Rf ${SCRIPT_PATH}/stateDir

# Switch to Aiven project
avn project switch ${PROJECT_NAME}

# Create Kafka service
avn service create \
    --service-type kafka \
    --cloud azure-westeurope \
    --plan business-4 \
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

printf "\n\nOutput SpringBoot application-aiven.yaml\n\n"

KSTREAM_CONFIG="${SCRIPT_PATH}/src/main/resources/application-aiven.yaml"
printf "kafka:\n" > ${KSTREAM_CONFIG}
printf "  serviceUri: %s:%s\n" ${SVC_HOST} ${SVC_PORT} >> ${KSTREAM_CONFIG}
printf "  username: %s\n" ${SVC_USERNAME} >> ${KSTREAM_CONFIG}
printf "  password: %s\n" ${SVC_PASSWORD} >> ${KSTREAM_CONFIG}
printf "  schemaRegistryUri: https://%s:%s@%s:%s\n" ${SVC_USERNAME} ${SVC_PASSWORD} ${SR_HOST} ${SR_PORT} >> ${KSTREAM_CONFIG}

printf "\n\n"

PRODUCER_CONFIG="${SCRIPT_PATH}/../kafka-producer-sample/src/main/resources/application-aiven.yaml"
printf "kafka:\n" > ${PRODUCER_CONFIG}
printf "  serviceUri: %s:%s\n" ${SVC_HOST} ${SVC_PORT} >> ${PRODUCER_CONFIG}
printf "  username: %s\n" ${SVC_USERNAME} >> ${PRODUCER_CONFIG}
printf "  password: %s\n" ${SVC_PASSWORD} >> ${PRODUCER_CONFIG}
printf "  schemaRegistryUri: https://%s:%s@%s:%s\n" ${SVC_USERNAME} ${SVC_PASSWORD} ${SR_HOST} ${SR_PORT} >> ${PRODUCER_CONFIG}

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


# Print IPs
printf "\n\nKafka Private IPS:\n"
avn service get --json \
    ${SERVICE_NAME} | jq '.connection_info.kafka'

printf "\n\nKafka Public IPS:\n"
SVC_HOST=$( avn service get ${SERVICE_NAME} --json | jq -r '.components[] | select(.route=="public" and .component=="kafka" and .kafka_authentication_method=="sasl").host' )
getent hosts ${SVC_HOST}