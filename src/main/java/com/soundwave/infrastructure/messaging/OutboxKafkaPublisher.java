package com.soundwave.infrastructure.messaging;

import com.soundwave.domain.entity.OutboxEvent;
import com.soundwave.infrastructure.persistence.repository.OutboxEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
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
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
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
    private final AtomicLong failedEvents = new AtomicLong(0);

    private Counter publishedCounter;
    private Counter publishFailedCounter;
    private Timer publishDuration;
    private DistributionSummary publishBatchSize;

    @PostConstruct
    void registerMetrics() {
        Gauge.builder("outbox.events.pending", pendingEvents, AtomicLong::get)
                .description("Number of pending outbox events")
                .register(meterRegistry);
        Gauge.builder("outbox.events.failed", failedEvents, AtomicLong::get)
                .description("Number of permanently failed outbox events")
                .register(meterRegistry);
        publishedCounter = Counter.builder("outbox.events.published")
                .description("Total outbox events successfully published to Kafka")
                .register(meterRegistry);
        publishFailedCounter = Counter.builder("outbox.events.publish.failures")
                .description("Total outbox publish attempts that failed (transient or permanent)")
                .register(meterRegistry);
        publishDuration = Timer.builder("outbox.publish.duration")
                .description("Latency of a single outbox publish call to Kafka")
                .register(meterRegistry);
        publishBatchSize = DistributionSummary.builder("outbox.publish.batch.size")
                .description("Number of events processed per publisher flush")
                .register(meterRegistry);
    }

    @Async("outboxExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOutboxEvent(OutboxEventSaved ignored) {
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
            var batch = outboxEventRepository.findPending(PageRequest.ofSize(BATCH_SIZE));
            publishBatchSize.record(batch.size());
            publishAll(batch);
        } finally {
            refreshMetrics();
            flushing.set(false);
        }
    }

    private void publishAll(List<OutboxEvent> events) {
        for (var event : events) {
            var sample = Timer.start(meterRegistry);
            try {
                var topic = resolveTopic(event);
                var envelope = toEnvelope(event);
                kafkaTemplate.send(topic, event.getAggregateId().toString(), envelope).get();
                event.markPublished();
                outboxEventRepository.save(event);
                sample.stop(publishDuration);
                publishedCounter.increment();
                log.atInfo()
                        .addKeyValue("eventId", event.getId())
                        .addKeyValue("eventType", event.getEventType())
                        .addKeyValue("topic", topic)
                        .log("Published outbox event to Kafka");
            } catch (Exception ex) {
                sample.stop(publishDuration);
                publishFailedCounter.increment();
                if (isNonRetryable(ex)) {
                    event.markFailed(ex.getMessage());
                    outboxEventRepository.save(event);
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

                // Let's break the loop to protect the order.
                break;
            }
        }
    }

    private boolean isNonRetryable(Exception ex) {
        // TODO: I kept this simple for the assignment; I should improve the error classification and add a DLQ strategy later.
        // This is also importent for keeping the events in order
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
            var envelope = new OutboxEnvelope(
                    event.getId().toString(),
                    event.getEventType(),
                    objectMapper.readTree(event.getPayload())
            );
            return objectMapper.writeValueAsString(envelope);
        } catch (JacksonException ex) {
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

    private void refreshMetrics() {
        pendingEvents.set(outboxEventRepository.countByPublishedFalseAndFailedFalse());
        failedEvents.set(outboxEventRepository.countByFailedTrue());
    }
}
