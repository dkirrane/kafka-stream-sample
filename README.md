# kafka-stream-sample

# Summary
[Aiven] [HIGH] Support ticket T-5AHCQ created: https://issues.apache.org/jira/browse/KAFKA-13467

Kafka Streams don't survive Aiven Kafka upgrade.
After upgrade, on first KStreams rebalance they never recover.
KStream Restore consumer never gets the new Aiven Broker IPs after an Aiven Kafka cluster rolling upgrade.

Reproduce steps below:

# 1. Setup
```bash

# Create Aiven Kafka service
./kafka-service-create.sh

```

# 2. Set App config
```bash

# Update application.yaml for both kafka-stream-sample && kafka-producer-sample applications
kafka:
  serviceUri: XXXXX
  username: XXXXX
  password: XXXXX
  schemaRegistryUri: XXXXX

```

# 3. Run Kafka stream instances (kafka-stream-sample)
```bash

# Terminal 1
mvn spring-boot:run -Dspring-boot.run.profiles="aiven" -Dspring-boot.run.arguments="--server.port=8081"

# Terminal 2
mvn spring-boot:run -Dspring-boot.run.profiles="aiven" -Dspring-boot.run.arguments="--server.port=8082"

# Terminal 3
mvn spring-boot:run -Dspring-boot.run.profiles="aiven" -Dspring-boot.run.arguments="--server.port=8083"

```

# 4. Run Kafka Producer (kafka-producer-sample)
```bash

# Terminal 4
mvn spring-boot:run -Dspring-boot.run.profiles="aiven"

```

# 5. Upgrade Aiven Kafka e.g. change service plan to trigger rolling VM upgrade
```bash

# Upgrade Aiven Kafka service Plan
./kafka-service-upgrade.sh

```

# 6. Wait for Aiven Kafka Service to go from REBALANCING -> REBUILDING -> RUNNING
This can take some time.

# 7. Verify
Verify the Producer & Streams app instances. They should continue to produce/consume as normal during & after the Aiven Kafka Service upgrade.

# 8. Trigger a Rebalance by running a new Kafka stream instance (kafka-stream-sample)
```bash

# Terminal 5
mvn spring-boot:run -Dspring-boot.run.profiles="aiven" -Dspring-boot.run.arguments="--server.port=8084"

```

# 9. Issue T-5AHCQ
- The KStream instances will State transition from RUNNING to REBALANCING
- The new KStream instance from step 8 above will work.
- The existing KStream instances from step 3 above will no longer consume messages. They stay in this state and never recover.
- The only workaround found to-date was to restart the existing KStream instances.


# 11. Cleanup
```bash

# Stop all applications
# Delete Aiven Kafka service
./kafka-service-delete.sh

```