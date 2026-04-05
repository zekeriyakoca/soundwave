package com.soundwave.infrastructure.messaging.consumer;

import com.soundwave.infrastructure.messaging.CatalogEventSchema;
import com.soundwave.domain.entity.ProcessedEvent;
import com.soundwave.infrastructure.messaging.OutboxEnvelope;
import com.soundwave.infrastructure.persistence.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@RequiredArgsConstructor
public abstract class IdempotentConsumer {

    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    protected abstract String consumerGroup();

    protected abstract void process(OutboxEnvelope envelope);

    @Transactional
    public void handle(String payload) {
        var envelope = parseEnvelope(payload);
        CatalogEventSchema.validateEnvelope(envelope);
        var eventId = envelope.eventId().strip();

        try {
            processedEventRepository.saveAndFlush(ProcessedEvent.create(eventId, consumerGroup()));
        } catch (DataIntegrityViolationException ex) {
            // TODO: I certainly need to check here later. Catch only duplicate-key violations (I need to check later. AI didn't help much in here)
            log.atDebug()
                    .addKeyValue("eventId", eventId)
                    .addKeyValue("consumerGroup", consumerGroup())
                    .log("Duplicate event, skipping");
            return;
        }

        process(envelope);
    }

    protected final String requiredText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Invalid event payload: missing " + fieldName);
        }
        return value;
    }

    protected final String requiredText(JsonNode node, String fieldName) {
        return requiredText(node.path(fieldName).asText(), fieldName);
    }

    private OutboxEnvelope parseEnvelope(String payload) {
        try {
            return objectMapper.readValue(payload, OutboxEnvelope.class);
        } catch (JacksonException ex) {
            throw new IllegalStateException("Failed to parse event payload", ex);
        }
    }
}
