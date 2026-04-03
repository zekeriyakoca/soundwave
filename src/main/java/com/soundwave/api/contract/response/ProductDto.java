package com.soundwave.api.contract.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ProductDto(
        UUID id,
        String title,
        String upc,
        LocalDate releaseDate,
        String genre,
        String status,
        MoneyDto price,
        UUID artistId,
        String artistName,
        List<TrackDto> tracks,
        Instant createdAt,
        Instant updatedAt
) {
}
