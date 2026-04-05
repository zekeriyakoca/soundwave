package com.soundwave.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent extends VersionedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;

    @Column(name = "aggregate_id", columnDefinition = "CHAR(36)", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(nullable = false, columnDefinition = "json")
    private String payload;

    @Column(nullable = false)
    private boolean published;

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(nullable = false)
    private boolean failed;

    @Column(name = "failure_reason")
    private String failureReason;

    private OutboxEvent(
            String aggregateType,
            java.util.UUID aggregateId,
            String eventType,
            String payload
    ) {
        this.aggregateType = requireNonBlank(aggregateType, "Aggregate type cannot be blank");
        this.aggregateId = Objects.requireNonNull(aggregateId, "Aggregate id cannot be null");
        this.eventType = requireNonBlank(eventType, "Event type cannot be blank");
        this.payload = requireNonBlank(payload, "Payload cannot be blank");
        this.published = false;
    }

    public static OutboxEvent create(
            String aggregateType,
            java.util.UUID aggregateId,
            String eventType,
            String payload
    ) {
        return new OutboxEvent(
                aggregateType,
                aggregateId,
                eventType,
                payload
        );
    }

    public void markPublished() {
        this.published = true;
        this.failed = false;
        this.failureReason = null;
        this.publishedAt = Instant.now();
    }

    public void markFailed(String reason) {
        this.published = false;
        this.failed = true;
        this.failureReason = reason;
    }

    private static String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.strip();
    }
}
