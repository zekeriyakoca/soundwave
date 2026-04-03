package com.soundwave.api.contract.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record ReorderTracksRequest(
        @NotEmpty List<UUID> trackOrder
) {
}
