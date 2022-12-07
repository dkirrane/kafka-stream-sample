package com.github.dkirrane.kproducer.topologies;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.ClientDnsLookup;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueBytesStoreSupplier;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Slf4j
@Component
public class SampleStream {

    public static String SASL_JAAS_TEMPLATE = "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"%s\" password=\"%s\";";

    @Value("${spring.application.name}")
    private String appName;

    @Value("${server.port}")
    private int serverPort;

    @Value( "${kafka.serviceUri}" )
    private String serviceUri;

    @Value( "${kafka.username}" )
    private String username;

    @Value( "${kafka.password}" )
    private String password;

    @Value( "${kafka.schemaRegistryUri}" )
    private String schemaRegistryUri;

    @Value( "${app.inputTopic}" )
    private String inputTopic;

    @Value( "${app.outputTopic}" )
    private String outputTopic;

    public Properties createConfig() throws Exception {
        Properties props = new Properties();

         /* Set the JVM TTL in case that solves the Aiven DNS cache issue */
        java.security.Security.setProperty("networkaddress.cache.ttl" , "1");

        props.put(StreamsConfig.APPLICATION_ID_CONFIG, appName);
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, serviceUri);

        /* Set admin DNS lookup strategy */
//        props.put(CommonClientConfigs.CLIENT_DNS_LOOKUP_CONFIG, ClientDnsLookup.USE_ALL_DNS_IPS.toString());
        props.put(CommonClientConfigs.CLIENT_DNS_LOOKUP_CONFIG, ClientDnsLookup.RESOLVE_CANONICAL_BOOTSTRAP_SERVERS_ONLY.toString());

        /* Set consumer DNS lookup strategy */
        props.put(CommonClientConfigs.METADATA_MAX_AGE_CONFIG, 500);
//        props.put(StreamsConfig.consumerPrefix(CommonClientConfigs.CLIENT_DNS_LOOKUP_CONFIG), ClientDnsLookup.USE_ALL_DNS_IPS.toString());
        props.put(StreamsConfig.consumerPrefix(CommonClientConfigs.CLIENT_DNS_LOOKUP_CONFIG), ClientDnsLookup.RESOLVE_CANONICAL_BOOTSTRAP_SERVERS_ONLY.toString());
        props.put(StreamsConfig.consumerPrefix(CommonClientConfigs.METADATA_MAX_AGE_CONFIG), 500);

        /* Set restore-consumer DNS lookup strategy */
//        props.put(StreamsConfig.restoreConsumerPrefix(CommonClientConfigs.CLIENT_DNS_LOOKUP_CONFIG), ClientDnsLookup.USE_ALL_DNS_IPS.toString());
        props.put(StreamsConfig.restoreConsumerPrefix(CommonClientConfigs.CLIENT_DNS_LOOKUP_CONFIG), ClientDnsLookup.RESOLVE_CANONICAL_BOOTSTRAP_SERVERS_ONLY.toString());
        props.put(StreamsConfig.restoreConsumerPrefix(CommonClientConfigs.METADATA_MAX_AGE_CONFIG), 500);

        props.put(StreamsConfig.SECURITY_PROTOCOL_CONFIG, SecurityProtocol.SASL_SSL.name);
        props.put(SaslConfigs.SASL_JAAS_CONFIG, String.format(SASL_JAAS_TEMPLATE, username, password));
        props.put(SaslConfigs.SASL_MECHANISM, "PLAIN");

        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUri);
        props.put(AbstractKafkaSchemaSerDeConfig.AUTO_REGISTER_SCHEMAS, true);

        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        props.put(StreamsConfig.STATE_DIR_CONFIG, String.format("./stateDir/%s%s", appName, serverPort));

        return props;
    }

    public Topology createTopology() {
        final StreamsBuilder streamsBuilder = new StreamsBuilder();

        log.info("Topics: {} {}", inputTopic, outputTopic);

        // Create State Store#
        final String storeName = "sampleStateStore";
        StoreBuilder<KeyValueStore<String, String>> storeBuilder = createStateStore(storeName);

        // Add State Store to Topology
        streamsBuilder.addStateStore(storeBuilder);

        // Topology
        KStream<String, String> inputStream = streamsBuilder.stream(inputTopic, Consumed.as("kstream-source"));

        final KStream<String, String> outputStream = inputStream
                .peek((key, value) -> log.debug("input \t key: {} \t value: {}", key, value), Named.as("kstream-peek-source"))
                .transformValues(() -> new ValueTransformerWithKey<String, String, String>() {

                    private ProcessorContext context;
                    private KeyValueStore<String, String> stateStore;

                    @Override
                    public void init(ProcessorContext context) {
                        this.context = context;
                        if (context.getStateStore(storeName) == null) {
                            throw new AssertionError("State stores are not accessible: " + storeName);
                        }
                        this.stateStore = (KeyValueStore<String, String>) this.context.getStateStore(storeName);
                    }

                    @Override
                    public String transform(String readOnlyKey, String value) {
                        final String currentValue = stateStore.get(readOnlyKey);
                        log.debug("StateStore {} - Current Value {} - New Value {} - State", storeName, currentValue, value);
                        stateStore.put(readOnlyKey, value);
                        return value;
                    }

                    @Override
                    public void close() {

                    }
                }, Named.as("kstream-transform-values"), storeName)
                .peek((key, value) -> log.info("OUTPUT>>> \t key: {} \t value: {}", key, value), Named.as("kstream-peek-sink"));

        // NOT for production use
//        outputStream.print(Printed.<String, String>toSysOut().withLabel("engagementAgg"));

        outputStream.to(outputTopic, Produced.as("kstream-sink"));

        return streamsBuilder.build();
    }

    private StoreBuilder<KeyValueStore<String, String>> createStateStore(String stateStoreName) {
        KeyValueBytesStoreSupplier storeSupplier = Stores.persistentKeyValueStore(stateStoreName);

        StoreBuilder<KeyValueStore<String, String>> storeBuilder =
                Stores.keyValueStoreBuilder(storeSupplier,
                        Serdes.String(),
                        Serdes.String());

        return storeBuilder;
    }

}