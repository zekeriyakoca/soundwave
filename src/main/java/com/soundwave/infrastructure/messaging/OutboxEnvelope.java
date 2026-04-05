package com.soundwave.infrastructure.messaging;

import tools.jackson.databind.JsonNode;

public record OutboxEnvelope(
        String eventId,
        String eventType,
        JsonNode payload
) {
}
