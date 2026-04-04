package com.soundwave.api.contract.request;

import com.soundwave.api.validation.DistinctElements;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record ReorderTracksRequest(
        @NotEmpty
        @DistinctElements(message = "Track order must not contain duplicate ids")
        List<@NotNull UUID> trackOrder
) {
}
