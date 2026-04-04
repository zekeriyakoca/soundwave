package com.soundwave.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "processed_events")
@IdClass(ProcessedEvent.ProcessedEventId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProcessedEvent {

    @Id
    @Column(name = "event_id", length = 36)
    private String eventId;

    @Id
    @Column(name = "consumer_group", length = 100)
    private String consumerGroup;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    private ProcessedEvent(String eventId, String consumerGroup) {
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("Event id cannot be blank");
        }
        if (consumerGroup == null || consumerGroup.isBlank()) {
            throw new IllegalArgumentException("Consumer group cannot be blank");
        }
        this.eventId = eventId.strip();
        this.consumerGroup = consumerGroup.strip();
        this.processedAt = Instant.now();
    }

    public static ProcessedEvent create(String eventId, String consumerGroup) {
        return new ProcessedEvent(eventId, consumerGroup);
    }

    public static class ProcessedEventId implements Serializable {
        private String eventId;
        private String consumerGroup;

        protected ProcessedEventId() {}

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ProcessedEventId other)) return false;
            return Objects.equals(eventId, other.eventId) && Objects.equals(consumerGroup, other.consumerGroup);
        }

        @Override
        public int hashCode() {
            return Objects.hash(eventId, consumerGroup);
        }
    }
}
