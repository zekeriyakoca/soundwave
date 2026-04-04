package com.soundwave.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic productEventsTopic() {
        return TopicBuilder.name("catalog.product.events")
                .partitions(5)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic artistEventsTopic() {
        return TopicBuilder.name("catalog.artist.events")
                .partitions(1)
                .replicas(1)
                .build();
    }
}
