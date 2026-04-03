package com.soundwave.api.contract.request;

import com.soundwave.api.contract.response.MoneyDto;
import com.soundwave.domain.entity.Genre;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record CreateProductRequest(
        @NotBlank String title,
        @NotNull UUID artistId,
        String upc,
        LocalDate releaseDate,
        @NotNull Genre genre,
        @Valid MoneyDto price
) {
}
