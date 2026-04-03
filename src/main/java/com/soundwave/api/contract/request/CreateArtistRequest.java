package com.soundwave.api.contract.request;

import jakarta.validation.constraints.NotBlank;

public record CreateArtistRequest(
        @NotBlank String name,
        String bio
) {
}
