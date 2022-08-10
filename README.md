# kafka-stream-sample

# Reproduce
[Aiven] [HIGH] Support ticket T-5AHCQ created: https://issues.apache.org/jira/browse/KAFKA-13467


# Setup
```bash

# Create Aiven Kafka service
./create-kafka-service.sh

```

# Run Kafka stream instances (kafka-stream-sample)
```bash

# Terminal 1
mvn spring-boot:run -Drun.arguments="--server.port=8081"

# Terminal 2
mvn spring-boot:run -Drun.arguments="--server.port=8082"

# Terminal 3
mvn spring-boot:run -Drun.arguments="--server.port=8083"

```

# Run Kafka Producer (kafka-producer-sample)
```bash

# Terminal 4
mvn spring-boot:run

```

# 3. Upgrade Aiven Kafka e.g. change service plan

# 4. Kafka Streams app fails to recover