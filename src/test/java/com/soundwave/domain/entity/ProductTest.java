package com.soundwave.domain.entity;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductTest {

    @Nested
    class Publish {

        @Test
        void succeeds_whenDraftWithTracks() {
            var product = draftProductWithTwoTracks();

            product.publish();

            assertEquals(ProductStatus.PUBLISHED, product.getStatus());
        }

        @Test
        void succeeds_whenTakenDownWithTracks() {
            var product = draftProductWithTwoTracks();
            product.publish();
            product.takeDown();

            product.publish();

            assertEquals(ProductStatus.PUBLISHED, product.getStatus());
        }

        @Test
        void throws_whenNoTracks() {
            var product = Product.create(
                    "Rumours",
                    "123456789012",
                    LocalDate.of(1977, 2, 4),
                    Genre.ROCK,
                    Money.of(new BigDecimal("9.99"), "EUR"),
                    Artist.create("Fleetwood Mac", "bio")
            );

            var ex = assertThrows(IllegalStateException.class, product::publish);
            assertEquals("Cannot publish product without tracks", ex.getMessage());
        }

        @Test
        void throws_whenAlreadyPublished() {
            var product = publishedProductWithSingleTrack();

            assertThrows(IllegalStateException.class, product::publish);
        }
    }

    @Nested
    class TakeDown {

        @Test
        void succeeds_whenPublished() {
            var product = publishedProductWithSingleTrack();

            product.takeDown();

            assertEquals(ProductStatus.TAKEN_DOWN, product.getStatus());
        }

        @Test
        void throws_whenDraft() {
            var product = draftProductWithTwoTracks();

            assertThrows(IllegalStateException.class, product::takeDown);
        }
    }

    @Nested
    class RemoveTrack {

        @Test
        void throws_whenTrackNotFound() {
            var product = publishedProductWithSingleTrack();
            var unknownTrackId = UUID.randomUUID();

            var ex = assertThrows(IllegalArgumentException.class, () -> product.removeTrack(unknownTrackId));
            assertTrue(ex.getMessage().contains("Track not found"));
        }

        @Test
        void succeeds_whenDraftProductLastTrack() {
            var product = draftProductWithSingleTrack();
            var trackId = product.getTracks().getFirst().getId();

            product.removeTrack(trackId);

            assertTrue(product.getTracks().isEmpty());
        }

        @Test
        void throws_whenLastTrackOnPublishedProduct() {
            var product = publishedProductWithSingleTrack();
            var existingTrackId = product.getTracks().getFirst().getId();

            var ex = assertThrows(IllegalStateException.class, () -> product.removeTrack(existingTrackId));
            assertEquals("Published product must have at least one track", ex.getMessage());
        }
    }

    @Nested
    class UpdateMetadata {

        @Test
        void returnsFalse_whenNothingChanged() {
            var price = Money.of(new BigDecimal("9.99"), "EUR");
            var product = Product.create(
                    "Rumours",
                    "123456789012",
                    LocalDate.of(1977, 2, 4),
                    Genre.ROCK,
                    price,
                    Artist.create("Fleetwood Mac", "bio")
            );

            var changed = product.updateMetadata(
                    "Rumours",
                    "123456789012",
                    LocalDate.of(1977, 2, 4),
                    Genre.ROCK,
                    Money.of(new BigDecimal("9.99"), "EUR")
            );

            assertFalse(changed);
        }

        @Test
        void returnsTrue_whenChanged() {
            var product = Product.create(
                    "Rumours",
                    "123456789012",
                    LocalDate.of(1977, 2, 4),
                    Genre.ROCK,
                    Money.of(new BigDecimal("9.99"), "EUR"),
                    Artist.create("Fleetwood Mac", "bio")
            );

            var changed = product.updateMetadata(
                    "Rumours (Remastered)",
                    "123456789012",
                    LocalDate.of(1977, 2, 4),
                    Genre.ROCK,
                    Money.of(new BigDecimal("12.99"), "EUR")
            );

            assertTrue(changed);
        }
    }

    @Nested
    class ReassignArtist {

        @Test
        void returnsTrue_whenDifferentArtist() {
            var product = draftProductWithSingleTrack();
            var newArtist = Artist.create("Stevie Nicks", "bio");

            var changed = product.reassignArtist(newArtist);

            assertTrue(changed);
            assertEquals(newArtist.getId(), product.getArtist().getId());
        }

        @Test
        void returnsFalse_whenSameArtist() {
            var product = draftProductWithSingleTrack();
            var sameArtist = product.getArtist();

            var changed = product.reassignArtist(sameArtist);

            assertFalse(changed);
        }
    }

    @Nested
    class AddTrack {

        @Test
        void throws_whenDuplicateTrackNumber() {
            var product = draftProductWithTwoTracks();

            var ex = assertThrows(IllegalStateException.class, () ->
                    product.addTrack("You Make Loving Fun", 217000, 1, "USWB19900003")
            );
            assertTrue(ex.getMessage().contains("already exists"));
        }

        @Test
        void throws_whenDuplicateIsrc() {
            var product = draftProductWithTwoTracks();

            var ex = assertThrows(IllegalStateException.class, () ->
                    product.addTrack("You Make Loving Fun", 217000, 3, "USWB19900001")
            );
            assertTrue(ex.getMessage().contains("ISRC"));
        }
    }

    @Nested
    class ReorderTracks {

        @Test
        void appliesRequestedOrder() {
            var product = draftProductWithTwoTracks();
            var first = product.getTracks().get(0);
            var second = product.getTracks().get(1);

            product.reorderTracks(List.of(second.getId(), first.getId()));

            assertEquals(second.getId(), product.getTracks().get(0).getId());
            assertEquals(1, product.getTracks().get(0).getTrackNumber());
            assertEquals(first.getId(), product.getTracks().get(1).getId());
            assertEquals(2, product.getTracks().get(1).getTrackNumber());
        }

        @Test
        void throws_whenUnknownTrackId() {
            var product = draftProductWithTwoTracks();
            var first = product.getTracks().get(0);
            var unknown = UUID.randomUUID();

            var ex = assertThrows(IllegalArgumentException.class, () ->
                    product.reorderTracks(List.of(first.getId(), unknown))
            );
            assertTrue(ex.getMessage().contains("Unknown track id"));
        }

        @Test
        void throws_whenDuplicateTrackIds() {
            var product = draftProductWithTwoTracks();
            var first = product.getTracks().get(0);

            var ex = assertThrows(IllegalArgumentException.class, () ->
                    product.reorderTracks(List.of(first.getId(), first.getId()))
            );
            assertTrue(ex.getMessage().contains("Duplicate track ids"));
        }
    }

    private Product draftProductWithSingleTrack() {
        var product = Product.create(
                "Rumours",
                "123456789012",
                LocalDate.of(1977, 2, 4),
                Genre.ROCK,
                Money.of(new BigDecimal("9.99"), "EUR"),
                Artist.create("Fleetwood Mac", "bio")
        );
        product.addTrack("Dreams", 254000, 1, "USWB19900001");
        return product;
    }

    private Product publishedProductWithSingleTrack() {
        var product = draftProductWithSingleTrack();
        product.publish();
        return product;
    }

    private Product draftProductWithTwoTracks() {
        var product = Product.create(
                "Rumours",
                "123456789012",
                LocalDate.of(1977, 2, 4),
                Genre.ROCK,
                Money.of(new BigDecimal("9.99"), "EUR"),
                Artist.create("Fleetwood Mac", "bio")
        );
        product.addTrack("Second Hand News", 163000, 1, "USWB19900001");
        product.addTrack("Dreams", 254000, 2, "USWB19900002");
        return product;
    }
}
