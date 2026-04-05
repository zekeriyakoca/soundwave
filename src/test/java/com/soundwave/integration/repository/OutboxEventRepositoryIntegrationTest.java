package com.soundwave.integration.repository;

import com.soundwave.domain.entity.OutboxEvent;
import com.soundwave.integration.MariaDbIntegrationTestBase;
import com.soundwave.infrastructure.persistence.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OutboxEventRepositoryIntegrationTest extends MariaDbIntegrationTestBase {

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @BeforeEach
    void clean() {
        outboxEventRepository.deleteAll();
    }

    @Test
    void findPendingReturnsOnlyUnpublishedNonFailedEventsInOrder() {
        var event1 = outboxEventRepository.save(OutboxEvent.create(
                "Product",
                UUID.randomUUID(),
                "ProductPublished",
                "{\"productId\":\"p1\"}"
        ));
        var event2 = outboxEventRepository.save(OutboxEvent.create(
                "Product",
                UUID.randomUUID(),
                "ProductMetadataUpdated",
                "{\"productId\":\"p2\"}"
        ));

        var failed = OutboxEvent.create(
                "Artist",
                UUID.randomUUID(),
                "ArtistUpdated",
                "{\"artistId\":\"a1\"}"
        );
        failed.markFailed("boom");
        outboxEventRepository.save(failed);

        var published = OutboxEvent.create(
                "Product",
                UUID.randomUUID(),
                "TrackListUpdated",
                "{\"productId\":\"p3\"}"
        );
        published.markPublished();
        outboxEventRepository.save(published);

        var pending = outboxEventRepository.findPending(PageRequest.ofSize(10));

        assertEquals(2, pending.size());
        assertEquals(List.of(event1.getId(), event2.getId()), pending.stream().map(OutboxEvent::getId).toList());
        assertEquals(2L, outboxEventRepository.countByPublishedFalseAndFailedFalse());
        assertTrue(pending.stream().allMatch(e -> !e.isPublished() && !e.isFailed()));
    }
}
