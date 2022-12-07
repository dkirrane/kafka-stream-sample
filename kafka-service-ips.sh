#!/bin/bash
set -e pipefail

set -a; source .env; set +a
SCRIPT_PATH="$(dirname -- "${BASH_SOURCE[0]}")"

# Switch to Aiven project
avn project switch ${PROJECT_NAME}

# Print IPs
IPS_LOG="${SCRIPT_PATH}/logs/ips.log"
[ -d ${SCRIPT_PATH}/logs ] || mkdir ${SCRIPT_PATH}/logs
touch ${IPS_LOG}

DATE=$( date '+%Y-%m-%d %H:%M:%S' )
SVC_HOST=$( avn service get ${SERVICE_NAME} --json | jq -r '.components[] | select(.route=="public" and .component=="kafka" and .kafka_authentication_method=="sasl").host' )
SVC_PLAN=$( avn service get ${SERVICE_NAME} --json | jq -r '.plan' )

printf "\n\n\n# Date: ${DATE}\n" 2>&1 | tee -a ${IPS_LOG}
printf "# Service: ${SVC_HOST}\n" 2>&1 | tee -a ${IPS_LOG}
printf "# Plan: ${SVC_PLAN}\n" 2>&1 | tee -a ${IPS_LOG}
printf "\n## Kafka Private IPS:\n" 2>&1 | tee -a ${IPS_LOG}
avn service get --json \
    ${SERVICE_NAME} | jq -r '.connection_info.kafka[] | .' 2>&1 | tee -a ${IPS_LOG}

printf "\n## Kafka Public IPS:\n" 2>&1 | tee -a ${IPS_LOG}
getent hosts ${SVC_HOST} 2>&1 | tee -a ${IPS_LOG}
