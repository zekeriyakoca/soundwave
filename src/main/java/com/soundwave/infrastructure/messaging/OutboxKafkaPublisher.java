package com.soundwave.infrastructure.messaging;

import com.soundwave.domain.entity.OutboxEvent;
import com.soundwave.infrastructure.persistence.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxKafkaPublisher {

    private static final String PRODUCT_TOPIC = "catalog.product.events";
    private static final String ARTIST_TOPIC = "catalog.artist.events";

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Async("outboxExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOutboxEvent(OutboxEvent event) {
        publishAll(outboxEventRepository.findAllByPublishedFalseOrderByCreatedAtAscIdAsc());
    }

    @Scheduled(fixedDelay = 5000)
    public void retryStaleEvents() {
        var stale = outboxEventRepository.findAllByPublishedFalseAndCreatedAtBeforeOrderByCreatedAtAscIdAsc(
                Instant.now().minusSeconds(10)
        );
        if (!stale.isEmpty()) {
            log.atInfo().addKeyValue("count", stale.size()).log("Retrying stale outbox events");
            publishAll(stale);
        }
    }

    private void publishAll(List<OutboxEvent> events) {
        for (var event : events) {
            try {
                var topic = resolveTopic(event);
                kafkaTemplate.send(topic, event.getAggregateId().toString(), event.getPayload()).get();
                event.markPublished();
                outboxEventRepository.save(event);
                log.atInfo()
                        .addKeyValue("eventId", event.getId())
                        .addKeyValue("eventType", event.getEventType())
                        .addKeyValue("topic", topic)
                        .log("Published outbox event to Kafka");
            } catch (Exception ex) {
                log.atWarn()
                        .setCause(ex)
                        .addKeyValue("eventId", event.getId())
                        .addKeyValue("eventType", event.getEventType())
                        .log("Failed to publish outbox event, will retry");
                break;
            }
        }
    }

    private String resolveTopic(OutboxEvent event) {
        return switch (event.getAggregateType()) {
            case "Product" -> PRODUCT_TOPIC;
            case "Artist" -> ARTIST_TOPIC;
            default -> throw new IllegalArgumentException("Unknown aggregate type: " + event.getAggregateType());
        };
    }
}
