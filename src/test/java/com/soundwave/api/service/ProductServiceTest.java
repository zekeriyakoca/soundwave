package com.soundwave.api.service;

import com.soundwave.api.contract.MoneyDto;
import com.soundwave.api.contract.request.AddTrackRequest;
import com.soundwave.api.contract.request.ReassignArtistRequest;
import com.soundwave.api.contract.request.ReorderTracksRequest;
import com.soundwave.api.contract.request.UpdateProductMetadataRequest;
import com.soundwave.domain.dto.DomainErrorCode;
import com.soundwave.domain.entity.Artist;
import com.soundwave.domain.entity.Genre;
import com.soundwave.domain.entity.Money;
import com.soundwave.domain.entity.Product;
import com.soundwave.domain.entity.ProductStatus;
import com.soundwave.infrastructure.persistence.repository.ArtistRepository;
import com.soundwave.infrastructure.persistence.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ArtistRepository artistRepository;

    @Mock
    private OutboxService outboxService;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository, artistRepository, outboxService);
    }

    @Nested
    class UpdateMetadata {

        @Test
        void returnsNotFound_whenProductMissing() {
            var id = UUID.randomUUID();
            when(productRepository.findById(id)).thenReturn(Optional.empty());
            var request = new UpdateProductMetadataRequest(
                    "Rumours", "123456789012", LocalDate.of(1977, 2, 4),
                    Genre.ROCK, new MoneyDto(new BigDecimal("9.99"), "EUR")
            );

            var result = productService.updateProductMetadata(id, request);

            assertFalse(result.isSuccess());
            assertEquals(DomainErrorCode.NOT_FOUND, result.getError().code());
        }

        @Test
        void sendsEvent_whenPublishedProductChanged() {
            var product = publishedProduct();
            when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
            var request = new UpdateProductMetadataRequest(
                    "Rumours (Remastered)",
                    product.getUpc(),
                    product.getReleaseDate(),
                    product.getGenre(),
                    new MoneyDto(product.getPrice().getAmount(), product.getPrice().getCurrency())
            );

            var result = productService.updateProductMetadata(product.getId(), request);

            assertTrue(result.isSuccess());
            verify(outboxService).saveProductMetadataUpdated(product);
        }

        @Test
        void skipsEvent_whenNothingChanged() {
            var product = publishedProduct();
            when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
            var request = new UpdateProductMetadataRequest(
                    product.getTitle(),
                    product.getUpc(),
                    product.getReleaseDate(),
                    product.getGenre(),
                    new MoneyDto(product.getPrice().getAmount(), product.getPrice().getCurrency())
            );

            var result = productService.updateProductMetadata(product.getId(), request);

            assertTrue(result.isSuccess());
            verify(outboxService, never()).saveProductMetadataUpdated(product);
        }

        @Test
        void skipsEvent_whenDraftProductChanged() {
            var product = draftProductWithTrack();
            when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
            var request = new UpdateProductMetadataRequest(
                    "Rumours (Draft Update)",
                    product.getUpc(),
                    product.getReleaseDate(),
                    product.getGenre(),
                    new MoneyDto(product.getPrice().getAmount(), product.getPrice().getCurrency())
            );

            var result = productService.updateProductMetadata(product.getId(), request);

            assertTrue(result.isSuccess());
            verify(outboxService, never()).saveProductMetadataUpdated(product);
        }
    }

    @Nested
    class ReassignArtist {

        @Test
        void returnsNotFound_whenProductMissing() {
            var id = UUID.randomUUID();
            when(productRepository.findById(id)).thenReturn(Optional.empty());

            var result = productService.reassignArtist(id, new ReassignArtistRequest(UUID.randomUUID()));

            assertFalse(result.isSuccess());
            assertEquals(DomainErrorCode.NOT_FOUND, result.getError().code());
        }

        @Test
        void returnsNotFound_whenArtistMissing() {
            var product = draftProductWithTrack();
            var newArtistId = UUID.randomUUID();
            when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
            when(artistRepository.existsById(newArtistId)).thenReturn(false);

            var result = productService.reassignArtist(product.getId(), new ReassignArtistRequest(newArtistId));

            assertFalse(result.isSuccess());
            assertEquals(DomainErrorCode.NOT_FOUND, result.getError().code());
        }

        @Test
        void sendsEvent_whenPublishedProductReassigned() {
            var product = publishedProduct();
            var newArtist = Artist.create("Stevie Nicks", "bio");
            when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
            when(artistRepository.existsById(newArtist.getId())).thenReturn(true);
            when(artistRepository.getReferenceById(newArtist.getId())).thenReturn(newArtist);

            var result = productService.reassignArtist(product.getId(), new ReassignArtistRequest(newArtist.getId()));

            assertTrue(result.isSuccess());
            verify(outboxService).saveProductMetadataUpdated(product);
        }

        @Test
        void skipsEvent_whenSameArtist() {
            var product = publishedProduct();
            var sameArtist = product.getArtist();
            when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
            when(artistRepository.existsById(sameArtist.getId())).thenReturn(true);
            when(artistRepository.getReferenceById(sameArtist.getId())).thenReturn(sameArtist);

            var result = productService.reassignArtist(product.getId(), new ReassignArtistRequest(sameArtist.getId()));

            assertTrue(result.isSuccess());
            verify(outboxService, never()).saveProductMetadataUpdated(product);
        }

        @Test
        void skipsEvent_whenDraftProductReassigned() {
            var product = draftProductWithTrack();
            var newArtist = Artist.create("Stevie Nicks", "bio");
            when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
            when(artistRepository.existsById(newArtist.getId())).thenReturn(true);
            when(artistRepository.getReferenceById(newArtist.getId())).thenReturn(newArtist);

            var result = productService.reassignArtist(product.getId(), new ReassignArtistRequest(newArtist.getId()));

            assertTrue(result.isSuccess());
            verify(outboxService, never()).saveProductMetadataUpdated(product);
        }
    }

    @Nested
    class Publish {

        @Test
        void sendsEvent_whenProductPublished() {
            var product = draftProductWithTrack();
            when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));

            var result = productService.publishProduct(product.getId());

            assertTrue(result.isSuccess());
            assertEquals(ProductStatus.PUBLISHED, product.getStatus());
            verify(outboxService).saveProductPublished(product);
        }
    }

    @Nested
    class TakeDown {

        @Test
        void sendsEvent_whenProductTakenDown() {
            var product = publishedProduct();
            when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));

            var result = productService.takeDownProduct(product.getId());

            assertTrue(result.isSuccess());
            assertEquals(ProductStatus.TAKEN_DOWN, product.getStatus());
            verify(outboxService).saveProductTakenDown(product);
        }
    }

    @Nested
    class TrackList {

        @Test
        void sendsEvent_whenTrackAddedToPublishedProduct() {
            var product = publishedProduct();
            when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
            var request = new AddTrackRequest("The Chain", 269000, 2, "USWB19900002");

            var result = productService.addTrack(product.getId(), request);

            assertTrue(result.isSuccess());
            verify(outboxService).saveTrackListUpdated(product);
        }

        @Test
        void skipsEvent_whenTrackAddedToDraftProduct() {
            var product = draftProductWithTrack();
            when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
            var request = new AddTrackRequest("The Chain", 269000, 2, "USWB19900002");

            var result = productService.addTrack(product.getId(), request);

            assertTrue(result.isSuccess());
            verify(outboxService, never()).saveTrackListUpdated(product);
        }

        @Test
        void sendsEvent_whenTrackRemovedFromPublishedProduct() {
            var product = publishedProductWithTwoTracks();
            when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
            var trackIdToRemove = product.getTracks().get(1).getId();

            var result = productService.removeTrack(product.getId(), trackIdToRemove);

            assertTrue(result.isSuccess());
            verify(outboxService).saveTrackListUpdated(product);
        }

        @Test
        void skipsEvent_whenTrackRemovedFromDraftProduct() {
            var product = draftProductWithTwoTracks();
            when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
            var trackIdToRemove = product.getTracks().get(1).getId();

            var result = productService.removeTrack(product.getId(), trackIdToRemove);

            assertTrue(result.isSuccess());
            verify(outboxService, never()).saveTrackListUpdated(product);
        }

        @Test
        void sendsEvent_whenTracksReorderedOnPublishedProduct() {
            var product = publishedProductWithTwoTracks();
            when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
            var firstTrackId = product.getTracks().get(0).getId();
            var secondTrackId = product.getTracks().get(1).getId();

            var request = new ReorderTracksRequest(List.of(secondTrackId, firstTrackId));
            var result = productService.reorderTracks(product.getId(), request);

            assertTrue(result.isSuccess());
            verify(outboxService).saveTrackListUpdated(product);
            assertEquals(secondTrackId, product.getTracks().getFirst().getId());
        }

        @Test
        void skipsEvent_whenTracksReorderedOnDraftProduct() {
            var product = draftProductWithTwoTracks();
            when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
            var firstTrackId = product.getTracks().get(0).getId();
            var secondTrackId = product.getTracks().get(1).getId();

            var request = new ReorderTracksRequest(List.of(secondTrackId, firstTrackId));
            var result = productService.reorderTracks(product.getId(), request);

            assertTrue(result.isSuccess());
            verify(outboxService, never()).saveTrackListUpdated(product);
        }
    }

    private Product publishedProduct() {
        var product = draftProductWithTrack();
        product.publish();
        return product;
    }

    private Product publishedProductWithTwoTracks() {
        var product = draftProductWithTrack();
        product.addTrack("The Chain", 269000, 2, "USWB19900002");
        product.publish();
        return product;
    }

    private Product draftProductWithTrack() {
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

    private Product draftProductWithTwoTracks() {
        var product = draftProductWithTrack();
        product.addTrack("The Chain", 269000, 2, "USWB19900002");
        return product;
    }
}
