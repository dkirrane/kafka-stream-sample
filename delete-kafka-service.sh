#!/bin/bash
set -e

# Switch to Aiven project
avn project switch avaya-d337

SERVICE_NAME="kafka-sample"

# Create Kafka service
avn service terminate ${SERVICE_NAME}