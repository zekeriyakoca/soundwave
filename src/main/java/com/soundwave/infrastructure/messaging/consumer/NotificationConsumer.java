package com.soundwave.infrastructure.messaging.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soundwave.infrastructure.persistence.repository.ProcessedEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificationConsumer extends IdempotentConsumer {

    public NotificationConsumer(ProcessedEventRepository repo, ObjectMapper mapper) {
        super(repo, mapper);
    }

    @KafkaListener(topics = "catalog.product.events", groupId = "notification-group")
    public void listen(@Payload String payload) {
        handle(payload);
    }

    @Override
    protected String consumerGroup() {
        return "notification-group";
    }

    @Override
    protected void process(String eventType, JsonNode payload) {
        if ("ProductPublished".equals(eventType)) {
            log.atInfo()
                    .addKeyValue("productId", payload.path("payload").path("productId").asText())
                    .addKeyValue("artistName", payload.path("payload").path("artistName").asText())
                    .addKeyValue("title", payload.path("payload").path("title").asText())
                    .log("[Notification] New release from artist");
        }
    }
}
