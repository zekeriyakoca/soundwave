package com.soundwave.infrastructure.messaging.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soundwave.infrastructure.persistence.repository.ProcessedEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CacheInvalidationConsumer extends IdempotentConsumer {

    private final CacheManager cacheManager;

    public CacheInvalidationConsumer(ProcessedEventRepository repo, ObjectMapper mapper, CacheManager cacheManager) {
        super(repo, mapper);
        this.cacheManager = cacheManager;
    }

    @KafkaListener(topics = "catalog.product.events", groupId = "cache-invalidation-group")
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
    protected void process(String eventType, JsonNode payload) {
        switch (eventType) {
            case "ProductMetadataUpdated", "TrackListUpdated", "ProductPublished", "ProductTakenDown" -> {
                var productId = payload.path("payload").path("productId").asText();
                evict("products", productId);
            }
            case "ArtistUpdated" -> {
                var artistId = payload.path("payload").path("artistId").asText();
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
