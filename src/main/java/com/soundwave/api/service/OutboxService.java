package com.soundwave.api.service;

import com.soundwave.domain.entity.Artist;
import com.soundwave.domain.entity.OutboxEvent;
import com.soundwave.domain.entity.Product;
import com.soundwave.infrastructure.messaging.payload.ArtistUpdatedPayload;
import com.soundwave.infrastructure.messaging.payload.MoneyPayload;
import com.soundwave.infrastructure.messaging.payload.ProductPayload;
import com.soundwave.infrastructure.messaging.payload.TrackListUpdatedPayload;
import com.soundwave.infrastructure.messaging.payload.TrackPayload;
import com.soundwave.infrastructure.messaging.CatalogEventSchema;
import com.soundwave.infrastructure.messaging.OutboxEventSaved;
import com.soundwave.infrastructure.persistence.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private static final String PRODUCT_AGGREGATE = "Product";
    private static final String ARTIST_AGGREGATE = "Artist";
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(propagation = Propagation.MANDATORY)
    public void saveProductPublished(Product product) {
        save(PRODUCT_AGGREGATE, product.getId(), CatalogEventSchema.PRODUCT_PUBLISHED, toProductPayload(product));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void saveProductTakenDown(Product product) {
        save(PRODUCT_AGGREGATE, product.getId(), CatalogEventSchema.PRODUCT_TAKEN_DOWN, toProductPayload(product));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void saveProductMetadataUpdated(Product product) {
        save(PRODUCT_AGGREGATE, product.getId(), CatalogEventSchema.PRODUCT_METADATA_UPDATED, toProductPayload(product));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void saveTrackListUpdated(Product product) {
        var payload = new TrackListUpdatedPayload(
                product.getId(),
                product.getArtist().getId(),
                product.getArtist().getName(),
                product.getTrackCount(),
                product.getTracks().stream()
                        .map(track -> new TrackPayload(
                                track.getId(),
                                track.getTitle(),
                                track.getDurationMs(),
                                track.getTrackNumber(),
                                track.getIsrc()
                        ))
                        .toList()
        );
        save(PRODUCT_AGGREGATE, product.getId(), CatalogEventSchema.TRACK_LIST_UPDATED, payload);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void saveArtistUpdated(Artist artist) {
        var payload = new ArtistUpdatedPayload(
                artist.getId(),
                artist.getName(),
                artist.getBio()
        );
        save(ARTIST_AGGREGATE, artist.getId(), CatalogEventSchema.ARTIST_UPDATED, payload);
    }

    private void save(String aggregateType, UUID aggregateId, String eventType, Object payload) {
        CatalogEventSchema.validateEventType(eventType);
        var outboxEvent = OutboxEvent.create(
                aggregateType,
                aggregateId,
                eventType,
                writeJson(payload)
        );
        outboxEventRepository.save(outboxEvent);
        eventPublisher.publishEvent(new OutboxEventSaved());
    }

    private String writeJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JacksonException ex) {
            throw new IllegalStateException("Could not serialize outbox payload", ex);
        }
    }

    private ProductPayload toProductPayload(Product product) {
        return new ProductPayload(
                product.getId(),
                product.getTitle(),
                product.getArtist().getId(),
                product.getArtist().getName(),
                product.getUpc(),
                product.getGenre().name(),
                product.getTrackCount(),
                toMoney(product)
        );
    }

    private MoneyPayload toMoney(Product product) {
        if (product.getPrice() == null) {
            return null;
        }
        return new MoneyPayload(
                product.getPrice().getAmount(),
                product.getPrice().getCurrency()
        );
    }
}
