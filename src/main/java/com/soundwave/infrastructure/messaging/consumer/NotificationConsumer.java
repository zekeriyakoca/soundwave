package com.soundwave.infrastructure.messaging.consumer;

import com.soundwave.infrastructure.messaging.CatalogEventSchema;
import com.soundwave.infrastructure.messaging.OutboxEnvelope;
import com.soundwave.infrastructure.persistence.repository.ProcessedEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

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
    protected void process(OutboxEnvelope envelope) {
        var body = envelope.payload();
        if (CatalogEventSchema.PRODUCT_PUBLISHED.equals(envelope.eventType())) {
            log.atInfo()
                    .addKeyValue("productId", requiredText(body, "productId"))
                    .addKeyValue("artistName", body.path("artistName").asText())
                    .addKeyValue("title", body.path("title").asText())
                    .log("[Notification] New release from artist");
        }
    }
}
