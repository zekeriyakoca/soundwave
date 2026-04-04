package com.soundwave.domain.dto;

import java.util.UUID;

public record TrackPayload(
        UUID trackId,
        String title,
        int durationMs,
        int trackNumber,
        String isrc
) {
}
