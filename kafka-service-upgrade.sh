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
bash "$SCRIPT_PATH/kafka-service-ips.sh"
