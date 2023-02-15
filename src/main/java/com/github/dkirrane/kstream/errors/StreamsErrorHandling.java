package com.github.dkirrane.kstream.errors;


import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.RecordTooLargeException;
import org.apache.kafka.streams.errors.DeserializationExceptionHandler;
import org.apache.kafka.streams.errors.ProductionExceptionHandler;
import org.apache.kafka.streams.errors.StreamsException;
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler;
import org.apache.kafka.streams.processor.ProcessorContext;

import java.util.Map;

@Slf4j
public class StreamsErrorHandling {
    //This is for learning purposes only!
    static boolean throwErrorNow = true;

    public static class StreamsDeserializationErrorHandler implements DeserializationExceptionHandler {
        int errorCounter = 0;

        @Override
        public DeserializationHandlerResponse handle(ProcessorContext context,
                                                     ConsumerRecord<byte[], byte[]> record,
                                                     Exception exception) {
            log.warn("Exception caught during Deserialization, " +
                            "taskId: {}, topic: {}, partition: {}, offset: {}",
                    context.taskId(), record.topic(), record.partition(), record.offset(),
                    exception);

            if (errorCounter++ < 25) {
                return DeserializationHandlerResponse.CONTINUE;
            }
            return DeserializationHandlerResponse.FAIL;
        }

        @Override
        public void configure(Map<String, ?> configs) {
        }
    }

    public static class StreamsRecordProducerErrorHandler implements ProductionExceptionHandler {
        @Override
        public ProductionExceptionHandlerResponse handle(ProducerRecord<byte[], byte[]> record,
                                                         Exception exception) {
            log.warn("Exception caught producing record, " +
                            "topic: {}, partition: {}, record key: {}, record value: {}, exception: {}", record.topic(), record.partition(), record.key(), record.value(), exception);
            if (exception instanceof RecordTooLargeException) {
                return ProductionExceptionHandlerResponse.CONTINUE;
            }
            return ProductionExceptionHandlerResponse.FAIL;
        }

        @Override
        public void configure(Map<String, ?> configs) {
        }
    }

    public static class StreamsCustomUncaughtExceptionHandler implements StreamsUncaughtExceptionHandler {
        @Override
        public StreamThreadExceptionResponse handle(Throwable exception) {
            log.error("StreamsUncaughtExceptionHandler", exception);
            if (exception instanceof StreamsException) {
                Throwable originalException = exception.getCause();
                if (originalException.getMessage().equals("Retryable transient error")) {
                    return StreamThreadExceptionResponse.REPLACE_THREAD;
                }
            }
            return StreamThreadExceptionResponse.SHUTDOWN_CLIENT;
        }
    }

}