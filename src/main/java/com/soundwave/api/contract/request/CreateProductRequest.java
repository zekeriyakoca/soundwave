package com.soundwave.api.contract.request;

import com.soundwave.api.contract.MoneyDto;
import com.soundwave.domain.entity.Genre;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.util.UUID;

public record CreateProductRequest(
        @NotBlank String title,
        @NotNull UUID artistId,
        @Pattern(regexp = "^\\d{12}$", message = "UPC must be exactly 12 digits")
        String upc,
        LocalDate releaseDate,
        @NotNull Genre genre,
        @Valid MoneyDto price
) {
}
