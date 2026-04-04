package com.soundwave.infrastructure.persistence.repository;

import com.soundwave.domain.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findAllByPublishedFalseOrderByCreatedAtAscIdAsc();

    List<OutboxEvent> findAllByPublishedFalseAndCreatedAtBeforeOrderByCreatedAtAscIdAsc(Instant before);
}
