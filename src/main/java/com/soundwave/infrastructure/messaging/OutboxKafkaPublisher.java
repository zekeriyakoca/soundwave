package com.soundwave.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soundwave.domain.entity.OutboxEvent;
import com.soundwave.infrastructure.persistence.repository.OutboxEventRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.SerializationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxKafkaPublisher {

    private static final String PRODUCT_TOPIC = "catalog.product.events";
    private static final String ARTIST_TOPIC = "catalog.artist.events";
    private static final int BATCH_SIZE = 50;

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final AtomicBoolean flushing = new AtomicBoolean(false);
    private final AtomicLong pendingEvents = new AtomicLong(0);

    @PostConstruct
    void registerMetrics() {
        Gauge.builder("outbox.events.pending", pendingEvents, AtomicLong::get)
                .description("Number of pending outbox events")
                .register(meterRegistry);
    }

    @Async("outboxExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOutboxEvent(OutboxEvent ignored) {
        flush();
    }

    @Scheduled(fixedDelay = 5000)
    public void retryPending() {
        flush();
    }

    private void flush() {
        if (!flushing.compareAndSet(false, true)) {
            return;
        }
        try {
            refreshPendingMetric();
            var batch = outboxEventRepository.findPending(PageRequest.ofSize(BATCH_SIZE));
            publishAll(batch);
        } finally {
            refreshPendingMetric();
            flushing.set(false);
        }
    }

    private void publishAll(List<OutboxEvent> events) {
        for (var event : events) {
            try {
                var topic = resolveTopic(event);
                var envelope = toEnvelope(event);
                kafkaTemplate.send(topic, event.getAggregateId().toString(), envelope).get();
                event.markPublished();
                outboxEventRepository.save(event);
                meterRegistry.counter("outbox.events.published").increment();
                log.atInfo()
                        .addKeyValue("eventId", event.getId())
                        .addKeyValue("eventType", event.getEventType())
                        .addKeyValue("topic", topic)
                        .log("Published outbox event to Kafka");
            } catch (Exception ex) {
                if (isNonRetryable(ex)) {
                    event.markFailed(ex.getMessage());
                    outboxEventRepository.save(event);
                    meterRegistry.counter("outbox.events.failed").increment();
                    log.atError()
                            .setCause(ex)
                            .addKeyValue("eventId", event.getId())
                            .addKeyValue("eventType", event.getEventType())
                            .log("Marked outbox event as failed");
                    continue;
                }
                log.atWarn()
                        .setCause(ex)
                        .addKeyValue("eventId", event.getId())
                        .addKeyValue("eventType", event.getEventType())
                        .log("Transient publish error, will retry later");
                break;
            }
        }
        refreshPendingMetric();
    }

    private boolean isNonRetryable(Exception ex) {
        // TODO: I kept this simple for the assignment; I should improve the error classification and add a DLQ strategy later.
        var root = rootCause(ex);
        return root instanceof IllegalArgumentException
                || root instanceof IllegalStateException
                || root instanceof SerializationException;
    }

    private Throwable rootCause(Throwable ex) {
        var current = ex;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private String toEnvelope(OutboxEvent event) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "eventId", event.getId().toString(),
                    "eventType", event.getEventType(),
                    "payload", objectMapper.readTree(event.getPayload())
            ));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to build event envelope", ex);
        }
    }

    private String resolveTopic(OutboxEvent event) {
        return switch (event.getAggregateType()) {
            case "Product" -> PRODUCT_TOPIC;
            case "Artist" -> ARTIST_TOPIC;
            default -> throw new IllegalArgumentException("Unknown aggregate type: " + event.getAggregateType());
        };
    }

    private void refreshPendingMetric() {
        pendingEvents.set(outboxEventRepository.countByPublishedFalseAndFailedFalse());
    }
}
