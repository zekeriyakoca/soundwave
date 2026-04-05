package com.soundwave.infrastructure.persistence.repository;

import com.soundwave.domain.entity.OutboxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    @Query("SELECT e FROM OutboxEvent e WHERE e.published = false AND e.failed = false ORDER BY e.createdAt, e.id")
    List<OutboxEvent> findPending(Pageable limit);

    @Query("SELECT count(e) FROM OutboxEvent e WHERE e.published = false AND e.failed = false")
    long countByPublishedFalseAndFailedFalse();

    @Query("SELECT count(e) FROM OutboxEvent e WHERE e.failed = true")
    long countByFailedTrue();
}
