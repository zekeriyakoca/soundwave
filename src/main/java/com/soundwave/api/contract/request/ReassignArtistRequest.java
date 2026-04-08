package com.soundwave.api.contract.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReassignArtistRequest(@NotNull UUID artistId) {
}
