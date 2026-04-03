package com.soundwave.api.contract.request;

import com.soundwave.api.contract.response.MoneyDto;
import com.soundwave.domain.entity.Genre;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record UpdateProductRequest(
        @NotBlank String title,
        String upc,
        LocalDate releaseDate,
        @NotNull Genre genre,
        @Valid MoneyDto price
) {
}
