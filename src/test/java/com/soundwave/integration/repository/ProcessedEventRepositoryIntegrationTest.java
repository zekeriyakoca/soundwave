package com.soundwave.integration.repository;

import com.soundwave.domain.entity.ProcessedEvent;
import com.soundwave.integration.MariaDbIntegrationTestBase;
import com.soundwave.infrastructure.persistence.repository.ProcessedEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessedEventRepositoryIntegrationTest extends MariaDbIntegrationTestBase {

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clean() {
        processedEventRepository.deleteAll();
    }

    @Test
    void duplicateEventForSameConsumerGroupViolatesPrimaryKey() {
        processedEventRepository.saveAndFlush(ProcessedEvent.create("event-1", "search-index-group"));

        assertTrue(processedEventRepository.existsByEventIdAndConsumerGroup("event-1", "search-index-group"));
        assertThrows(DataIntegrityViolationException.class, () ->
                jdbcTemplate.update(
                        "insert into processed_events(event_id, consumer_group, processed_at) values (?, ?, CURRENT_TIMESTAMP)",
                        "event-1",
                        "search-index-group"
                )
        );
    }
}
