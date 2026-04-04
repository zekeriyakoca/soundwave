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
    protected void process(String eventType, JsonNode payload) {
        switch (eventType) {
            case "ProductPublished" -> log.atInfo()
                    .addKeyValue("productId", payload.path("payload").path("productId").asText())
                    .addKeyValue("title", payload.path("payload").path("title").asText())
                    .log("[SearchIndex] Indexing published product");
            case "ProductTakenDown" -> log.atInfo()
                    .addKeyValue("productId", payload.path("payload").path("productId").asText())
                    .log("[SearchIndex] Removing taken down product from index");
            default -> log.atDebug()
                    .addKeyValue("eventType", eventType)
                    .log("[SearchIndex] Ignoring event");
        }
    }
}
