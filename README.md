# Summary
[Aiven] [HIGH] Support ticket `T-5AHCQ` created: https://issues.apache.org/jira/browse/KAFKA-13467

- Kafka Streams don't survive Aiven Kafka upgrade.
- After upgrade, on first rebalance KStreams instances never recover.
- KStream restore-consumer never gets the new Aiven Broker IPs after an Aiven Kafka cluster rolling upgrade.

## Reproduce steps

### 1. Setup
```bash

# Clone KStream & Producer samples
git clone git@github.com:dkirrane/kafka-stream-sample.git
git clone git@github.com:dkirrane/kafka-producer-sample.git

# Create Aiven Kafka service (edit .env)
cd kafka-stream-sample
./kafka-service-create.sh

```

### 2. Verify App config
> `kafka-service-create.sh` outputs the required connection details to `application-aiven.yaml` for both the `kafka-stream-sample` && `kafka-producer-sample` applications
```yaml

# application-aiven.yaml
kafka:
  serviceUri: XXXXX
  username: XXXXX
  password: XXXXX
  schemaRegistryUri: XXXXX

```

### 3. Run Kafka Streams instances (kafka-stream-sample)
```bash

# Terminal 1
mvn spring-boot:run -Dspring-boot.run.profiles="aiven" -Dspring-boot.run.arguments="--server.port=8081" -Dnetworkaddress.cache.ttl=1

# Terminal 2
mvn spring-boot:run -Dspring-boot.run.profiles="aiven" -Dspring-boot.run.arguments="--server.port=8082" -Dnetworkaddress.cache.ttl=1

# Terminal 3
mvn spring-boot:run -Dspring-boot.run.profiles="aiven" -Dspring-boot.run.arguments="--server.port=8083" -Dnetworkaddress.cache.ttl=1

```

### 4. Run Kafka Producer (kafka-producer-sample)
```bash

# Terminal 4
cd kafka-producer-sample
mvn spring-boot:run -Dspring-boot.run.profiles="aiven"

```

### 5. Upgrade Aiven Kafka e.g. change service plan to trigger a service rolling upgrade
```bash

# Upgrade Aiven Kafka service Plan
./kafka-service-upgrade.sh

```

### 6. Wait for Aiven Kafka Service to go from REBALANCING -> REBUILDING -> RUNNING
This can take some time.

### 7. Verify
Verify the Producer & Streams app instances.
> NOTE: KStream instances should continue to consume/produce as normal during & after the Aiven Kafka Service upgrade.

### 8. Trigger a Rebalance by running a new Kafka stream instance (kafka-stream-sample)
```bash

# Terminal 5
mvn spring-boot:run -Dspring-boot.run.profiles="aiven" -Dspring-boot.run.arguments="--server.port=8084" -Dnetworkaddress.cache.ttl=1

```

### 9. Issue T-5AHCQ
> Issue occurs after Triggering a KStream Rebalance
- The KStream instances will State transition from `RUNNING` to `REBALANCING`
- The new KStream instance from step `8` above will work.
- All existing KStream instances from step `3` above will no longer consume messages. They stay in this state and never recover.

### 10. Logs

> `INFO` logs for the Kafka Stream instances that are stuck, you'll see logs like below in the console for each instance:
```log

2022-12-07 | 14:49:44.796 |  INFO | kstream-sample-ae1c26e9-e16b-4d49-a97d-3511c9089f0a-StreamThread-1                                   | org.apache.kafka.clients.NetworkClient
            | [Consumer clientId=kstream-sample-ae1c26e9-e16b-4d49-a97d-3511c9089f0a-StreamThread-1-restore-consumer, groupId=null] Disconnecting from node 2 due to socket connection setup timeout. The timeout value is 34662 ms.

```

> `DEBUG` logs - see `./log` directory, you'll see logs like below for the `restore-consumer` stuck trying to connect to old Broker IPs forever:
```log

2022-12-07 | 16:24:08.414 | DEBUG | kstream-sample-d7f6398b-f50e-4b6c-a09f-e2980eb1db93-StreamThread-1                                   | org.apache.kafka.clients.consumer.internals.Fetcher          | [Consumer clientId=kstream-sample-d7f6398b-f50e-4b6c-a09f-e2980eb1db93-StreamThread-1-restore-consumer, groupId=null] Sending ListOffsetRequest ListOffsetsRequestData(replicaId=-1, isolationLevel=0, topics=[ListOffsetsTopic(name='kstream-sample-sampleStateStore-changelog', partitions=[ListOffsetsPartition(partitionIndex=2, currentLeaderEpoch=0, timestamp=-2, maxNumOffsets=1)])]) to broker 20.56.132.112:25624 (id: 3 rack: null)
2022-12-07 | 16:24:08.414 | DEBUG | kstream-sample-d7f6398b-f50e-4b6c-a09f-e2980eb1db93-StreamThread-1                                   | org.apache.kafka.clients.ClientUtils                         | Resolved host 20.56.132.112 as 20.56.132.112
2022-12-07 | 16:24:08.415 | DEBUG | kstream-sample-d7f6398b-f50e-4b6c-a09f-e2980eb1db93-StreamThread-1                                   | org.apache.kafka.clients.NetworkClient                       | [Consumer clientId=kstream-sample-d7f6398b-f50e-4b6c-a09f-e2980eb1db93-StreamThread-1-restore-consumer, groupId=null] Initiating connection to node 20.56.132.112:25624 (id: 3 rack: null) using address /20.56.132.112

```

### 11. Attempts to resolve
> The only workaround found to-date was to restart the existing KStream instances.
> We've tried the following but in all cases the problem persists:
```java

        /* We've tried both of the DNS lookup strategies - restore-consumer DNS lookup strategy */
        props.put(StreamsConfig.restoreConsumerPrefix(CommonClientConfigs.CLIENT_DNS_LOOKUP_CONFIG), ClientDnsLookup.USE_ALL_DNS_IPS.toString());

        props.put(StreamsConfig.restoreConsumerPrefix(CommonClientConfigs.CLIENT_DNS_LOOKUP_CONFIG), ClientDnsLookup.RESOLVE_CANONICAL_BOOTSTRAP_SERVERS_ONLY.toString());

        /* We've tried reducing metadata max age */
        props.put(StreamsConfig.restoreConsumerPrefix(CommonClientConfigs.METADATA_MAX_AGE_CONFIG), 500);

        /* We've set JVM TTL */
        java.security.Security.setProperty("networkaddress.cache.ttl" , "1");

```

## Cleanup
```bash

# Stop all applications
# Delete Aiven Kafka service
./kafka-service-destroy.sh

```