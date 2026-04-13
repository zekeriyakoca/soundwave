package com.soundwave.infrastructure.messaging.consumer;

import com.soundwave.infrastructure.messaging.CatalogEventSchema;
import com.soundwave.infrastructure.messaging.OutboxEnvelope;
import com.soundwave.infrastructure.persistence.repository.ProcessedEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
public class CacheInvalidationConsumer extends IdempotentConsumer {

    private final CacheManager cacheManager;

    public CacheInvalidationConsumer(ProcessedEventRepository repo, ObjectMapper mapper, CacheManager cacheManager) {
        super(repo, mapper);
        this.cacheManager = cacheManager;
    }

    @KafkaListener(topics = "catalog.product.events", groupId = "cache-invalidation-group", concurrency = "5")
    public void listenProducts(@Payload String payload) {
        handle(payload);
    }

    @KafkaListener(topics = "catalog.artist.events", groupId = "cache-invalidation-group")
    public void listenArtists(@Payload String payload) {
        handle(payload);
    }

    @Override
    protected String consumerGroup() {
        return "cache-invalidation-group";
    }

    @Override
    protected void process(OutboxEnvelope envelope) {
        var body = envelope.payload();
        switch (envelope.eventType()) {
            case CatalogEventSchema.PRODUCT_METADATA_UPDATED,
                 CatalogEventSchema.TRACK_LIST_UPDATED,
                 CatalogEventSchema.PRODUCT_PUBLISHED,
                 CatalogEventSchema.PRODUCT_TAKEN_DOWN -> {
                var productId = requiredText(body, "productId");
                evict("products", productId);
            }
            case CatalogEventSchema.ARTIST_UPDATED -> {
                var artistId = requiredText(body, "artistId");
                evict("artists", artistId);
            }
        }
    }

    private void evict(String cacheName, String key) {
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
            log.atInfo()
                    .addKeyValue("cache", cacheName)
                    .addKeyValue("key", key)
                    .log("[CacheInvalidation] Evicted cache entry");
        }
    }
}
