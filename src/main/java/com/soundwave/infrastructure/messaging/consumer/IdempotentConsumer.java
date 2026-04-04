package com.soundwave.infrastructure.messaging.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soundwave.domain.entity.ProcessedEvent;
import com.soundwave.infrastructure.persistence.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
public abstract class IdempotentConsumer {

    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void handle(String payload) {
        var root = parse(payload);
        var eventId = root.path("eventId").asText();
        var eventType = root.path("eventType").asText();
        if (eventId == null || eventId.isBlank() || eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("Invalid event payload");
        }

        try {
            processedEventRepository.saveAndFlush(ProcessedEvent.create(eventId, consumerGroup()));
        } catch (DataIntegrityViolationException ex) {
            log.atDebug()
                    .addKeyValue("eventId", eventId)
                    .addKeyValue("consumerGroup", consumerGroup())
                    .log("Duplicate event, skipping");
            return;
        }

        process(eventType, root);
    }

    protected abstract String consumerGroup();

    protected abstract void process(String eventType, JsonNode payload);

    private JsonNode parse(String payload) {
        try {
            return objectMapper.readTree(payload);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse event payload", ex);
        }
    }
}
