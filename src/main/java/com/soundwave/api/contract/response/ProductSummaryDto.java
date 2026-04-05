package com.soundwave.api.contract.response;

import com.soundwave.api.contract.MoneyDto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ProductSummaryDto(
        UUID id,
        String title,
        String upc,
        LocalDate releaseDate,
        String genre,
        String status,
        MoneyDto price,
        UUID artistId,
        String artistName,
        int trackCount,
        Instant createdAt,
        Instant updatedAt
) {
}
