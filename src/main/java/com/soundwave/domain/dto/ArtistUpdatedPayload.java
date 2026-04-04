package com.soundwave.domain.dto;

import java.util.UUID;

public record ArtistUpdatedPayload(
        UUID artistId,
        String name,
        String bio
) {
}
