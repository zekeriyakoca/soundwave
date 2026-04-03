package com.soundwave.api.contract.response;

import java.util.UUID;

public record TrackDto(UUID id, String title, Integer durationMs, Integer trackNumber, String isrc) {
}
