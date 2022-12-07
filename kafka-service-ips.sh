#!/bin/bash
set -e
# set -euxo pipefail

set -a; source .env; set +a
SCRIPT_PATH="$(dirname -- "${BASH_SOURCE[0]}")"

# Switch to Aiven project
avn project switch ${PROJECT_NAME}

# Print IPs
IPS_LOG="${SCRIPT_PATH}/logs/ips.log"

SVC_HOST=$( avn service get ${SERVICE_NAME} --json | jq -r '.components[] | select(.route=="public" and .component=="kafka" and .kafka_authentication_method=="sasl").host' )

printf "\n\n\n# ${SVC_HOST}\n" >> ${IPS_LOG}
printf "\n## Kafka Private IPS:\n" >> ${IPS_LOG}
avn service get --json \
    ${SERVICE_NAME} | jq -r '.connection_info.kafka[] | .' >> ${IPS_LOG}

printf "\n\n## Kafka Public IPS:\n" >> ${IPS_LOG}
getent hosts ${SVC_HOST} >> ${IPS_LOG}
