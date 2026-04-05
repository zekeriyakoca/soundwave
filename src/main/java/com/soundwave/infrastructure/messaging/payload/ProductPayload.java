package com.soundwave.infrastructure.messaging.payload;

import java.util.UUID;

public record ProductPayload(
        UUID productId,
        String title,
        UUID artistId,
        String artistName,
        String upc,
        String genre,
        int trackCount,
        MoneyPayload price
) {
}
