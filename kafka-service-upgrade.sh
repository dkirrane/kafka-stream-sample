#!/bin/bash
set -e
# set -euxo pipefail

# Switch to Aiven project
avn project switch avaya-d337

# T-5AHCQ - Kafka Streams restore consumer DNS issue on Aiven Kafka cluster rolling upgrades
SERVICE_NAME="kstreams-issue"

# Upgrade Kafka Service Plan
avn service update \
    --plan business-4 \
    ${SERVICE_NAME}

# Wait for Kafka service to reach the RUNNING state
avn service wait ${SERVICE_NAME}
