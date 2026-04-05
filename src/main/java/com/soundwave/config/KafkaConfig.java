package com.soundwave.config;

import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

@Configuration
public class KafkaConfig {

    private static final String PRODUCT_EVENTS_TOPIC = "catalog.product.events";
    private static final String ARTIST_EVENTS_TOPIC = "catalog.artist.events";

    @Bean
    public NewTopic productEventsTopic() {
        return TopicBuilder.name(PRODUCT_EVENTS_TOPIC)
                .partitions(5)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic artistEventsTopic() {
        return TopicBuilder.name(ARTIST_EVENTS_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic productEventsDltTopic() {
        return TopicBuilder.name(PRODUCT_EVENTS_TOPIC + ".dlt")
                .partitions(5)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic artistEventsDltTopic() {
        return TopicBuilder.name(ARTIST_EVENTS_TOPIC + ".dlt")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public CommonErrorHandler kafkaErrorHandler(KafkaOperations<Object, Object> kafkaOperations) {
        var recoverer = new DeadLetterPublishingRecoverer(
                kafkaOperations,
                (record, ex) -> new TopicPartition(record.topic() + ".dlt", record.partition())
        );
        // Trade-off: I would add a jitter here, but I'm running out of time.
        var backOff = new ExponentialBackOff(2000L, 2.0);
        backOff.setMaxAttempts(3);
        var errorHandler = new DefaultErrorHandler(recoverer, backOff);
        errorHandler.addNotRetryableExceptions(IllegalArgumentException.class, IllegalStateException.class);
        return errorHandler;
    }
}
