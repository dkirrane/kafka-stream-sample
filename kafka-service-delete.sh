#!/bin/bash
set -euxo pipefail

# Switch to Aiven project
avn project switch avaya-d337

# T-5AHCQ - Kafka Streams restore consumer DNS issue on Aiven Kafka cluster rolling upgrades
SERVICE_NAME="kstreams-issue"

# Create Kafka service
avn service terminate ${SERVICE_NAME}