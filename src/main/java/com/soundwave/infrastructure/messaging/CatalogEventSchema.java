package com.soundwave.infrastructure.messaging;

import java.util.Set;

public final class CatalogEventSchema {

    public static final String PRODUCT_METADATA_UPDATED = "ProductMetadataUpdated";
    public static final String PRODUCT_PUBLISHED = "ProductPublished";
    public static final String PRODUCT_TAKEN_DOWN = "ProductTakenDown";
    public static final String TRACK_LIST_UPDATED = "TrackListUpdated";
    public static final String ARTIST_UPDATED = "ArtistUpdated";

    private static final Set<String> SUPPORTED_EVENT_TYPES = Set.of(
            PRODUCT_METADATA_UPDATED,
            PRODUCT_PUBLISHED,
            PRODUCT_TAKEN_DOWN,
            TRACK_LIST_UPDATED,
            ARTIST_UPDATED
    );

    private CatalogEventSchema() {
    }

    public static void validateEventType(String eventType) {
        var normalizedType = requiredText(eventType, "eventType");
        if (!SUPPORTED_EVENT_TYPES.contains(normalizedType)) {
            throw new IllegalArgumentException("Unsupported event type: " + eventType);
        }
    }

    public static void validateEnvelope(OutboxEnvelope envelope) {
        if (envelope == null) {
            throw new IllegalArgumentException("Invalid event payload: missing envelope");
        }
        requiredText(envelope.eventId(), "eventId");
        validateEventType(envelope.eventType());
        if (envelope.payload() == null) {
            throw new IllegalArgumentException("Invalid event payload: missing payload");
        }
    }

    private static String requiredText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Invalid event payload: missing " + fieldName);
        }
        return value.strip();
    }
}
