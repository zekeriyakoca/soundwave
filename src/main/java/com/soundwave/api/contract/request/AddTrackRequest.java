package com.soundwave.api.contract.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AddTrackRequest(
        @NotBlank String title,
        @NotNull @Positive Integer durationMs,
        @NotNull @Positive Integer trackNumber,
        String isrc
) {
}
