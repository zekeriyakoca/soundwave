package com.soundwave.integration.repository;

import com.soundwave.domain.entity.Artist;
import com.soundwave.domain.entity.Genre;
import com.soundwave.domain.entity.Money;
import com.soundwave.domain.entity.Product;
import com.soundwave.integration.MariaDbIntegrationTestBase;
import com.soundwave.infrastructure.persistence.repository.ArtistRepository;
import com.soundwave.infrastructure.persistence.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductRepositoryIntegrationTest extends MariaDbIntegrationTestBase {

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void clean() {
        productRepository.deleteAll();
        artistRepository.deleteAll();
    }

    @Nested
    class FindWithDetailsById {

        @Test
        void loadsArtistAndTracks() {
            var artist = artistRepository.save(Artist.create("Fleetwood Mac", "bio"));

            var product1 = Product.create(
                    "Rumours",
                    "123456789012",
                    LocalDate.of(1977, 2, 4),
                    Genre.ROCK,
                    Money.of(new BigDecimal("9.99"), "EUR"),
                    artist
            );
            product1.addTrack("Second Hand News", 163000, 1, "USWB19900001");
            product1.addTrack("Dreams", 254000, 2, "USWB19900002");

            var product2 = Product.create(
                    "Tusk",
                    "123456789013",
                    LocalDate.of(1979, 10, 12),
                    Genre.ROCK,
                    Money.of(new BigDecimal("11.99"), "EUR"),
                    artist
            );
            product2.addTrack("Over & Over", 251000, 1, "USWB19900003");

            productRepository.saveAndFlush(product1);
            productRepository.saveAndFlush(product2);

            var loaded = productRepository.findWithDetailsById(product1.getId());

            assertTrue(loaded.isPresent());
            assertNotNull(loaded.get().getArtist());
            assertEquals(2, loaded.get().getTracks().size());
        }

        @Test
        void preservesTrackOrderAfterReorder() {
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
            product.addTrack("The Chain", 269000, 3, "USWB19900003");
            var saved = productRepository.saveAndFlush(product);

            var firstId = saved.getTracks().get(0).getId();
            var secondId = saved.getTracks().get(1).getId();
            var thirdId = saved.getTracks().get(2).getId();

            var fresh = productRepository.findWithDetailsById(saved.getId()).orElseThrow();
            fresh.reorderTracks(List.of(thirdId, firstId, secondId));
            productRepository.saveAndFlush(fresh);
            entityManager.clear();

            var loaded = productRepository.findWithDetailsById(saved.getId()).orElseThrow();
            var tracks = loaded.getTracks();

            assertEquals(3, tracks.size());
            assertEquals(thirdId, tracks.get(0).getId());
            assertEquals(1, tracks.get(0).getTrackNumber());
            assertEquals(firstId, tracks.get(1).getId());
            assertEquals(2, tracks.get(1).getTrackNumber());
            assertEquals(secondId, tracks.get(2).getId());
            assertEquals(3, tracks.get(2).getTrackNumber());
        }
    }

    @Nested
    class FindAllBy {

        @Test
        void returnsArtistAccessibleAfterSessionClose() {
            var artist = artistRepository.save(Artist.create("Fleetwood Mac", "bio"));

            for (int i = 0; i < 3; i++) {
                var product = Product.create(
                        "Album " + i,
                        null,
                        LocalDate.of(1977, 2, 4),
                        Genre.ROCK,
                        Money.of(new BigDecimal("9.99"), "EUR"),
                        artist
                );
                productRepository.save(product);
            }
            productRepository.flush();

            var page = productRepository.findAllBy(
                    PageRequest.of(0, 2, Sort.by("title"))
            );
            entityManager.clear();

            assertEquals(2, page.getContent().size());
            assertEquals(3, page.getTotalElements());
            for (var product : page.getContent()) {
                assertNotNull(product.getArtist().getName());
            }
        }
    }

    @Nested
    class UniqueConstraints {

        @Test
        void rejectsDuplicateUpc() {
            var artist = artistRepository.save(Artist.create("Fleetwood Mac", "bio"));

            var product1 = Product.create(
                    "Rumours",
                    "123456789012",
                    LocalDate.of(1977, 2, 4),
                    Genre.ROCK,
                    Money.of(new BigDecimal("9.99"), "EUR"),
                    artist
            );
            productRepository.saveAndFlush(product1);

            var product2 = Product.create(
                    "Tusk",
                    "123456789012",
                    LocalDate.of(1979, 10, 12),
                    Genre.ROCK,
                    Money.of(new BigDecimal("11.99"), "EUR"),
                    artist
            );

            assertThrows(DataIntegrityViolationException.class, () ->
                    productRepository.saveAndFlush(product2)
            );
        }
    }
}
