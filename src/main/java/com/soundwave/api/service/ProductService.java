package com.soundwave.api.service;

import com.soundwave.api.contract.DtoMapper;
import com.soundwave.api.contract.request.AddTrackRequest;
import com.soundwave.api.contract.request.CreateProductRequest;
import com.soundwave.api.contract.request.ReorderTracksRequest;
import com.soundwave.api.contract.request.UpdateProductRequest;
import com.soundwave.api.contract.response.PagedResponse;
import com.soundwave.api.contract.response.ProductDto;
import com.soundwave.api.contract.response.ProductSummaryDto;
import com.soundwave.domain.dto.DomainErrorCode;
import com.soundwave.domain.dto.Error;
import com.soundwave.domain.dto.Result;
import com.soundwave.domain.entity.Money;
import com.soundwave.domain.entity.Product;
import com.soundwave.infrastructure.persistence.repository.ArtistRepository;
import com.soundwave.infrastructure.persistence.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ArtistRepository artistRepository;

    @Cacheable(value = "products", key = "#id")
    @Transactional(readOnly = true)
    public Result<ProductDto> getProduct(UUID id) {
        var product = productRepository.findWithDetailsById(id);
        if (product.isEmpty()) {
            return Result.failure(new Error(DomainErrorCode.NOT_FOUND, "Product not found"));
        }
        return Result.success(DtoMapper.toDto(product.get()));
    }

    @Transactional(readOnly = true)
    public Result<PagedResponse<ProductSummaryDto>> listProducts(Pageable page) {
        var products = productRepository.findAllBy(page);
        var response = new PagedResponse<>(
                products.map(DtoMapper::toSummaryDto).stream().toList(),
                products.getNumber(),
                products.getSize(),
                products.getTotalElements(),
                products.getTotalPages()
        );
        return Result.success(response);
    }

    @Transactional
    public Result<ProductDto> createProduct(CreateProductRequest request) {
        var artist = artistRepository.findById(request.artistId());
        if (artist.isEmpty()) {
            return Result.failure(new Error(DomainErrorCode.NOT_FOUND, "Artist not found"));
        }

        var product = Product.create(
                request.title(),
                request.upc(),
                request.releaseDate(),
                request.genre(),
                toMoney(request.price()),
                artist.get()
        );

        var saved = productRepository.save(product);
        log.info("Created product {} for artist {}", saved.getId(), saved.getArtist().getId());
        return Result.success(DtoMapper.toDto(saved));
    }

    @CacheEvict(value = "products", key = "#id")
    @Transactional
    public Result<ProductDto> updateProduct(UUID id, UpdateProductRequest request) {
        var product = productRepository.findById(id);
        if (product.isEmpty()) {
            return Result.failure(new Error(DomainErrorCode.NOT_FOUND, "Product not found"));
        }
        product.get().updateMetadata(
                request.title(),
                request.upc(),
                request.releaseDate(),
                request.genre(),
                toMoney(request.price())
        );
        log.info("Updated product {}", product.get().getId());
        return Result.success(DtoMapper.toDto(product.get()));
    }

    @CacheEvict(value = "products", key = "#id")
    @Transactional
    public Result<ProductDto> publishProduct(UUID id) {
        var product = productRepository.findById(id);
        if (product.isEmpty()) {
            return Result.failure(new Error(DomainErrorCode.NOT_FOUND, "Product not found"));
        }
        product.get().publish();
        log.info("Published product {}", product.get().getId());
        return Result.success(DtoMapper.toDto(product.get()));
    }

    @CacheEvict(value = "products", key = "#id")
    @Transactional
    public Result<ProductDto> takeDownProduct(UUID id) {
        var product = productRepository.findById(id);
        if (product.isEmpty()) {
            return Result.failure(new Error(DomainErrorCode.NOT_FOUND, "Product not found"));
        }
        product.get().takeDown();
        log.info("Took down product {}", product.get().getId());
        return Result.success(DtoMapper.toDto(product.get()));
    }

    @CacheEvict(value = "products", key = "#productId")
    @Transactional
    public Result<ProductDto> addTrack(UUID productId, AddTrackRequest request) {
        var product = productRepository.findById(productId);
        if (product.isEmpty()) {
            return Result.failure(new Error(DomainErrorCode.NOT_FOUND, "Product not found"));
        }
        product.get().addTrack(
                request.title(),
                request.durationMs(),
                request.trackNumber(),
                request.isrc()
        );
        log.info("Added a track to product {}", product.get().getId());
        return Result.success(DtoMapper.toDto(product.get()));
    }

    @CacheEvict(value = "products", key = "#productId")
    @Transactional
    public Result<ProductDto> removeTrack(UUID productId, UUID trackId) {
        var product = productRepository.findById(productId);
        if (product.isEmpty()) {
            return Result.failure(new Error(DomainErrorCode.NOT_FOUND, "Product not found"));
        }
        product.get().removeTrack(trackId);
        log.info("Removed track {} from product {}", trackId, productId);
        return Result.success(DtoMapper.toDto(product.get()));
    }

    @CacheEvict(value = "products", key = "#productId")
    @Transactional
    public Result<ProductDto> reorderTracks(UUID productId, ReorderTracksRequest request) {
        var product = productRepository.findById(productId);
        if (product.isEmpty()) {
            return Result.failure(new Error(DomainErrorCode.NOT_FOUND, "Product not found"));
        }
        product.get().reorderTracks(request.trackOrder());
        log.info("Updated track order for product {}", productId);
        return Result.success(DtoMapper.toDto(product.get()));
    }

    private Money toMoney(com.soundwave.api.contract.response.MoneyDto dto) {
        if (dto == null) return null;
        return Money.of(dto.amount(), dto.currency());
    }
}
