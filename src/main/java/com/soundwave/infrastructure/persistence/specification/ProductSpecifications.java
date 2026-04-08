package com.soundwave.infrastructure.persistence.specification;

import com.soundwave.domain.entity.Product;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class ProductSpecifications {

    private ProductSpecifications() {}

    public static Specification<Product> hasArtistId(UUID artistId) {
        return (root, query, cb) ->
                artistId == null ? null : cb.equal(root.get("artist").get("id"), artistId);
    }
}
