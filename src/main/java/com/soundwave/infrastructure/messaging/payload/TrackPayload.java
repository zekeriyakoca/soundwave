package com.soundwave.infrastructure.messaging.payload;

import java.util.UUID;

public record TrackPayload(
        UUID trackId,
        String title,
        int durationMs,
        int trackNumber,
        String isrc
) {
}
