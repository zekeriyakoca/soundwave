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
public class SearchIndexConsumer extends IdempotentConsumer {

    public SearchIndexConsumer(ProcessedEventRepository repo, ObjectMapper mapper) {
        super(repo, mapper);
    }

    @KafkaListener(topics = "catalog.product.events", groupId = "search-index-group")
    public void listen(@Payload String payload) {
        handle(payload);
    }

    @Override
    protected String consumerGroup() {
        return "search-index-group";
    }

    @Override
    protected void process(OutboxEnvelope envelope) {
        var body = envelope.payload();
        switch (envelope.eventType()) {
            case CatalogEventSchema.PRODUCT_PUBLISHED -> log.atInfo()
                    .addKeyValue("productId", requiredText(body, "productId"))
                    .addKeyValue("title", body.path("title").asText())
                    .log("[SearchIndex] Indexing published product");
            case CatalogEventSchema.PRODUCT_TAKEN_DOWN -> log.atInfo()
                    .addKeyValue("productId", requiredText(body, "productId"))
                    .log("[SearchIndex] Removing taken down product from index");
            default -> log.atDebug()
                    .addKeyValue("eventType", envelope.eventType())
                    .log("[SearchIndex] Ignoring event");
        }
    }
}
