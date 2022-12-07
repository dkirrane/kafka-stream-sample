#!/bin/bash
set -euxo pipefail

set -a; source .env; set +a
SCRIPT_PATH="$(dirname -- "${BASH_SOURCE[0]}")"

# Switch to Aiven project
avn project switch ${PROJECT_NAME}

# Create Kafka service
avn service terminate ${SERVICE_NAME}