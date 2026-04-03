package com.soundwave.api.contract.response;

import java.time.Instant;
import java.util.UUID;

public record ArtistDto(UUID id, String name, String bio, Instant createdAt, Instant updatedAt) {
}
