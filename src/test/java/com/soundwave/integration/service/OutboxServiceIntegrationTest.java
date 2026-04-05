package com.soundwave.integration.service;

import com.soundwave.api.service.OutboxService;
import com.soundwave.domain.entity.Artist;
import com.soundwave.domain.entity.Genre;
import com.soundwave.domain.entity.Money;
import com.soundwave.domain.entity.Product;
import com.soundwave.integration.MariaDbIntegrationTestBase;
import com.soundwave.infrastructure.messaging.CatalogEventSchema;
import com.soundwave.infrastructure.persistence.repository.ArtistRepository;
import com.soundwave.infrastructure.persistence.repository.OutboxEventRepository;
import com.soundwave.infrastructure.persistence.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OutboxServiceIntegrationTest extends MariaDbIntegrationTestBase {

    @Autowired
    private OutboxService outboxService;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void clean() {
        outboxEventRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
        artistRepository.deleteAllInBatch();
    }

    @Test
    void saveProductPublishedPersistsPendingOutboxRow() {
        var product = persistedPublishedProduct();

        transactionTemplate.executeWithoutResult(status ->
                outboxService.saveProductPublished(product)
        );

        assertEquals(1L, outboxEventRepository.count());
        var event = outboxEventRepository.findAll().getFirst();
        assertEquals("Product", event.getAggregateType());
        assertEquals(CatalogEventSchema.PRODUCT_PUBLISHED, event.getEventType());
        assertEquals(product.getId(), event.getAggregateId());
        assertFalse(event.isPublished());
        assertFalse(event.isFailed());
        assertTrue(event.getPayload().contains("\"productId\":\"" + product.getId() + "\""));
    }

    @Test
    void saveTrackListUpdatedPersistsTrackPayload() throws Exception {
        var product = persistedPublishedProductWithTwoTracks();

        transactionTemplate.executeWithoutResult(status ->
                outboxService.saveTrackListUpdated(product)
        );

        var event = outboxEventRepository.findAll().getFirst();
        assertEquals(CatalogEventSchema.TRACK_LIST_UPDATED, event.getEventType());

        var payload = objectMapper.readTree(event.getPayload());
        assertEquals(product.getId().toString(), payload.path("productId").asText());
        assertEquals(2, payload.path("tracks").size());
        assertEquals(1, payload.path("tracks").get(0).path("trackNumber").asInt());
        assertEquals(2, payload.path("tracks").get(1).path("trackNumber").asInt());
    }

    private Product persistedPublishedProduct() {
        var artist = artistRepository.save(Artist.create("Fleetwood Mac", "bio"));
        var product = Product.create(
                "Rumours",
                "123456789012",
                LocalDate.of(1977, 2, 4),
                Genre.ROCK,
                Money.of(new BigDecimal("9.99"), "EUR"),
                artist
        );
        product.addTrack("Dreams", 254000, 1, "USWB19900001");
        product.publish();
        productRepository.saveAndFlush(product);
        return productRepository.findWithDetailsById(product.getId()).orElseThrow();
    }

    private Product persistedPublishedProductWithTwoTracks() {
        var artist = artistRepository.save(Artist.create("Fleetwood Mac", "bio"));
        var product = Product.create(
                "Rumours",
                "123456789012",
                LocalDate.of(1977, 2, 4),
                Genre.ROCK,
                Money.of(new BigDecimal("9.99"), "EUR"),
                artist
        );
        product.addTrack("Second Hand News", 163000, 1, "USWB19900001");
        product.addTrack("Dreams", 254000, 2, "USWB19900002");
        product.publish();
        productRepository.saveAndFlush(product);
        return productRepository.findWithDetailsById(product.getId()).orElseThrow();
    }
}
