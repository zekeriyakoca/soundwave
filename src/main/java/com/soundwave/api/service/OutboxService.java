package com.soundwave.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soundwave.domain.dto.ArtistUpdatedPayload;
import com.soundwave.domain.dto.MoneyPayload;
import com.soundwave.domain.dto.ProductPayload;
import com.soundwave.domain.dto.TrackListUpdatedPayload;
import com.soundwave.domain.dto.TrackPayload;
import com.soundwave.domain.entity.Artist;
import com.soundwave.domain.entity.OutboxEvent;
import com.soundwave.domain.entity.Product;
import com.soundwave.infrastructure.persistence.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private static final String PRODUCT_AGGREGATE = "Product";
    private static final String ARTIST_AGGREGATE = "Artist";

    private static final String PRODUCT_METADATA_UPDATED_EVENT = "ProductMetadataUpdated";
    private static final String PRODUCT_PUBLISHED_EVENT = "ProductPublished";
    private static final String PRODUCT_TAKEN_DOWN_EVENT = "ProductTakenDown";
    private static final String TRACK_LIST_UPDATED_EVENT = "TrackListUpdated";
    private static final String ARTIST_UPDATED_EVENT = "ArtistUpdated";

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(propagation = Propagation.MANDATORY)
    public void saveProductPublished(Product product) {
        save(PRODUCT_AGGREGATE, product.getId(), PRODUCT_PUBLISHED_EVENT, toProductPayload(product));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void saveProductTakenDown(Product product) {
        save(PRODUCT_AGGREGATE, product.getId(), PRODUCT_TAKEN_DOWN_EVENT, toProductPayload(product));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void saveProductMetadataUpdated(Product product) {
        save(PRODUCT_AGGREGATE, product.getId(), PRODUCT_METADATA_UPDATED_EVENT, toProductPayload(product));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void saveTrackListUpdated(Product product) {
        var payload = new TrackListUpdatedPayload(
                product.getId(),
                product.getArtist().getId(),
                product.getArtist().getName(),
                product.getTracks().size(),
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
        save(PRODUCT_AGGREGATE, product.getId(), TRACK_LIST_UPDATED_EVENT, payload);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void saveArtistUpdated(Artist artist) {
        var payload = new ArtistUpdatedPayload(
                artist.getId(),
                artist.getName(),
                artist.getBio()
        );
        save(ARTIST_AGGREGATE, artist.getId(), ARTIST_UPDATED_EVENT, payload);
    }

    private void save(String aggregateType, java.util.UUID aggregateId, String eventType, Object payload) {
        var outboxEvent = OutboxEvent.create(
                aggregateType,
                aggregateId,
                eventType,
                writeJson(payload)
        );
        outboxEventRepository.save(outboxEvent);
        eventPublisher.publishEvent(outboxEvent);
    }

    private String writeJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
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
                product.getTracks().size(),
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
