#!/bin/bash
set -e
# set -euxo pipefail

set -a; source .env; set +a
SCRIPT_PATH="$(dirname -- "${BASH_SOURCE[0]}")"

# Switch to Aiven project
avn project switch ${PROJECT_NAME}

# Upgrade Kafka Service Plan
avn service update \
    --plan business-8 \
    ${SERVICE_NAME}

sleep 5

# Wait for Kafka service to reach the RUNNING state
avn service wait ${SERVICE_NAME}

# Print IPs
printf "\n\nKafka Private IPS:\n"
avn service get --json \
    ${SERVICE_NAME} | jq -r '.connection_info.kafka'

printf "\n\nKafka Public IPS:\n"
SVC_HOST=$( avn service get ${SERVICE_NAME} --json | jq -r '.components[] | select(.route=="public" and .component=="kafka" and .kafka_authentication_method=="sasl").host' )
getent hosts ${SVC_HOST}
