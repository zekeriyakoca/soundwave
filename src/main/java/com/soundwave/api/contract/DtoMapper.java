package com.soundwave.api.contract;

import com.soundwave.api.contract.response.ArtistDto;
import com.soundwave.api.contract.response.ProductDto;
import com.soundwave.api.contract.response.ProductSummaryDto;
import com.soundwave.api.contract.response.TrackDto;
import com.soundwave.domain.entity.Artist;
import com.soundwave.domain.entity.Product;
import com.soundwave.domain.entity.Track;

public final class DtoMapper {

    private DtoMapper() {}

    public static ArtistDto toDto(Artist artist) {
        return new ArtistDto(
                artist.getId(),
                artist.getName(),
                artist.getBio(),
                artist.getCreatedAt(),
                artist.getUpdatedAt()
        );
    }

    public static ProductDto toDto(Product product) {
        var price = product.getPrice();
        return new ProductDto(
                product.getId(),
                product.getTitle(),
                product.getUpc(),
                product.getReleaseDate(),
                product.getGenre().name(),
                product.getStatus().name(),
                price != null ? new MoneyDto(price.getAmount(), price.getCurrency()) : null,
                product.getArtist().getId(),
                product.getArtist().getName(),
                product.getTracks().stream().map(DtoMapper::toDto).toList(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    public static ProductSummaryDto toSummaryDto(Product product) {
        var price = product.getPrice();
        return new ProductSummaryDto(
                product.getId(),
                product.getTitle(),
                product.getUpc(),
                product.getReleaseDate(),
                product.getGenre().name(),
                product.getStatus().name(),
                price != null ? new MoneyDto(price.getAmount(), price.getCurrency()) : null,
                product.getArtist().getId(),
                product.getArtist().getName(),
                product.getTrackCount(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    public static TrackDto toDto(Track track) {
        return new TrackDto(
                track.getId(),
                track.getTitle(),
                track.getDurationMs(),
                track.getTrackNumber(),
                track.getIsrc()
        );
    }
}
