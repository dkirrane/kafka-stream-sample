package com.github.dkirrane.kproducer;

import com.github.dkirrane.kproducer.topologies.SampleStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsMetadata;
import org.apache.kafka.streams.ThreadMetadata;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Collection;
import java.util.Properties;
import java.util.Set;

@Slf4j
@Component
public class KStreamSampleRunner {

    private final SampleStream sampleStream;
    private KafkaStreams stream;

    @Autowired
    public KStreamSampleRunner(SampleStream sampleStream) {
        this.sampleStream = sampleStream;
    }

    public KafkaStreams getStream() {
        return stream;
    }

    @PostConstruct
    public void postConstruct() throws Exception {
        log.info("Starting SampleStream");

        stream = startStream(sampleStream.createTopology(), sampleStream.createConfig());

        log.info("Started Kafka SampleStream");
    }

    @PreDestroy
    public void preDestroy() {
        log.info("Closing SampleStream");
        stream.close();
        log.info("SampleStream has been closed [{}]", stream.state());
    }

    private KafkaStreams startStream(Topology topology, Properties streamProps) {
        log.debug("SampleStream Topology: \n{}", topology.describe().toString());
        log.info("SampleStream config: \n{}", streamProps);
        KafkaStreams stream = new KafkaStreams(topology, streamProps);

        stream.setUncaughtExceptionHandler(new StreamsUncaughtExceptionHandler() {
            @Override
            public StreamThreadExceptionResponse handle(Throwable exception) {
                log.error("StreamsUncaughtExceptionHandler", exception);
                return StreamThreadExceptionResponse.SHUTDOWN_CLIENT;
            }
        });

        stream.start();
        log.info("SampleStream has started [{}] ", stream.state());

        final Set<ThreadMetadata> threadMetadata = stream.metadataForLocalThreads();
        for (ThreadMetadata threadMetadatum : threadMetadata) {
            System.out.println("\n\n\n\n\n");
            System.out.println("ThreadMetadata:");
            System.out.println("threadName = " + threadMetadatum.threadName());
            System.out.println("threadState = " + threadMetadatum.threadState());
            System.out.println("adminClientId = " + threadMetadatum.adminClientId());
            System.out.println("producerClientIds = " + threadMetadatum.producerClientIds());
            System.out.println("consumerClientId = " + threadMetadatum.consumerClientId());
            System.out.println("restoreConsumerClientId = " + threadMetadatum.restoreConsumerClientId());
            System.out.println("activeTasks = " + threadMetadatum.activeTasks());
            System.out.println("standbyTasks = " + threadMetadatum.standbyTasks());
        }

        final Collection<StreamsMetadata> streamsMetadata = stream.metadataForAllStreamsClients();
        for (StreamsMetadata streamsMetadatum : streamsMetadata) {
            System.out.println("\n\n\n\n\n");
            System.out.println("streamsMetadatum:");
            System.out.println("stateStoreNames = " + streamsMetadatum.stateStoreNames());
            System.out.println("topicPartitions = " + streamsMetadatum.topicPartitions());
            System.out.println("standbyStateStoreNames = " + streamsMetadatum.standbyStateStoreNames());
            System.out.println("standbyTopicPartitions = " + streamsMetadatum.standbyTopicPartitions());
        }

        return stream;
    }

}
