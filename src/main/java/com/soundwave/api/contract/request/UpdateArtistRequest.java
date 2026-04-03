package com.soundwave.api.contract.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateArtistRequest(
        @NotBlank String name,
        String bio
) {
}
