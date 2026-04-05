package com.soundwave.infrastructure.messaging.payload;

import java.util.UUID;

public record ArtistUpdatedPayload(
        UUID artistId,
        String name,
        String bio
) {
}
