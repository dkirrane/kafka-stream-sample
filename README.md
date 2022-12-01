# kafka-stream-sample

# Summary
[Aiven] [HIGH] Support ticket T-5AHCQ created: https://issues.apache.org/jira/browse/KAFKA-13467

Kafka Streams don't work with Aiven Kafka.
Restore consumer has DNS issues after a Aiven Kafka cluster rolling upgrade.
When KStreams instances rebalance they never recover.

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
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"

# Terminal 2
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8082"

# Terminal 3
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8083"

```

# 4. Run Kafka Producer (kafka-producer-sample)
```bash

# Terminal 4
mvn spring-boot:run

```

# 5. Upgrade Aiven Kafka e.g. change service plan to trigger rolling VM upgrade
```bash

# Upgrade Aiven Kafka service Plan
./kafka-service-upgrade.sh

```

# 6. Wait for Aiven Kafka Service to go from REBALANCING -> REBUILDING -> RUNNING
This can be slow.

# 7. Verify
Verify the Producer & Streams app instances. They should continue to produce/consume as normal during & after the Aiven Kafka Service upgrade.

# 8. Trigger a Rebalance by Running a new Kafka stream instance (kafka-stream-sample)
```bash

# Terminal 5
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8084"

# State transition from RUNNING to PARTITIONS_ASSIGNED
# 2022-12-01 | 13:40:18.285 |  INFO | kstream-sample-b5f9da3e-c78c-4c2e-8af6-c5a5fd32022e-StreamThread-1                                   | org.apache.kafka.streams.processor.internals.StreamThread    | stream-thread [kstream-sample-b5f9da3e-c78c-4c2e-8af6-c5a5fd32022e-StreamThread-1] State transition from RUNNING to PARTITIONS_ASSIGNED
# stream-client [kstream-sample-b5f9da3e-c78c-4c2e-8af6-c5a5fd32022e] State transition from RUNNING to REBALANCING
# 2022-12-01 | 13:40:22.592 |  INFO | kstream-sample-63e9867a-2747-48e9-8890-62579c4a1a4f-StreamThread-1                                   | o.apache.kafka.clients.consumer.internals.SubscriptionState  | [Consumer clientId=kstream-sample-63e9867a-2747-48e9-8890-62579c4a1a4f-StreamThread-1-restore-consumer, groupId=null] Seeking to EARLIEST offset of partition kstream-sample-sampleStateStore-changelog-3
```

# 9. Issue T-5AHCQ
- The KStream instances will State transition from RUNNING to REBALANCING
- The new KStream instance from step 8 above will work.
- The existing KStream instances from step 3 above will no longer consume messages. They stay in this state and never recover.
- The only workaround found to-date was to restart the existing KStream instances.

# 10. Logs
In existing KStream instances you will see logs:

- After Triggering KStream Rebalance  e.g.
`State transition from RUNNING to PARTITIONS_ASSIGNED`

- Restore Consumer Thread subscribes to changelog topic e.g.
`[Consumer clientId=kstream-sample-...-StreamThread-1-restore-consumer, groupId=null] ... Subscribed to partition(s): kstream-sample-sampleStateStore-changelog..`

- StoreChangelogReader stuck in Restoration in progress e.g.
`o.a.kafka.streams.processor.internals.StoreChangelogReader   | stream-thread [kstream-sample-...-StreamThread-1] Restoration in progress for X partitions`

- Check logs for restore-consumer connection only (requires DEBUG logs for org.apache.kafka.clients.NetworkClient)
`.*restore-consumer.*Initiating connection to node.*`


# 11. Cleanup
```bash

# Delete Aiven Kafka service
./kafka-service-delete.sh

```