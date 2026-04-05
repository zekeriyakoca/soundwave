package com.soundwave.infrastructure.messaging.payload;

import java.util.List;
import java.util.UUID;

public record TrackListUpdatedPayload(
        UUID productId,
        UUID artistId,
        String artistName,
        int trackCount,
        List<TrackPayload> tracks
) {
}
