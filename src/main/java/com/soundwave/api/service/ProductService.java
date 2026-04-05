package com.soundwave.api.service;

import com.soundwave.api.contract.DtoMapper;
import com.soundwave.api.contract.MoneyDto;
import com.soundwave.api.contract.request.AddTrackRequest;
import com.soundwave.api.contract.request.CreateProductRequest;
import com.soundwave.api.contract.request.ReorderTracksRequest;
import com.soundwave.api.contract.request.UpdateProductMetadataRequest;
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
    private final OutboxService outboxService;

    @Cacheable(value = "products", key = "#id", unless = "!#result.isSuccess()")
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
        var response = PagedResponse.from(productRepository.findAllBy(page).map(p -> DtoMapper.toSummaryDto(p)));
        return Result.success(response);
    }

    @Transactional
    public Result<ProductDto> createProductAsDraft(CreateProductRequest request) {
        if (!artistRepository.existsById(request.artistId())) {
            return Result.failure(new Error(DomainErrorCode.NOT_FOUND, "Artist not found"));
        }

        var artistRef = artistRepository.getReferenceById(request.artistId());
        var product = Product.create(
                request.title(),
                request.upc(),
                request.releaseDate(),
                request.genre(),
                toMoney(request.price()),
                artistRef
        );

        var saved = productRepository.save(product);
        log.info("Created product {}", saved.getId());
        return Result.success(DtoMapper.toDto(saved));
    }

    @CacheEvict(value = "products", key = "#id")
    @Transactional
    public Result<ProductDto> updateProductMetadata(UUID id, UpdateProductMetadataRequest request) {
        var product = productRepository.findById(id);
        if (product.isEmpty()) {
            return Result.failure(new Error(DomainErrorCode.NOT_FOUND, "Product not found"));
        }
        var existingProduct = product.get();
        var changed = existingProduct.updateMetadata(
                request.title(),
                request.upc(),
                request.releaseDate(),
                request.genre(),
                toMoney(request.price())
        );

        if (changed && existingProduct.isPublished()) {
            outboxService.saveProductMetadataUpdated(existingProduct);
        }

        if (changed) {
            log.info("Updated product metadata {}", existingProduct.getId());
        }

        return Result.success(DtoMapper.toDto(existingProduct));
    }

    @CacheEvict(value = "products", key = "#id")
    @Transactional
    public Result<ProductDto> publishProduct(UUID id) {
        var product = productRepository.findById(id);
        if (product.isEmpty()) {
            return Result.failure(new Error(DomainErrorCode.NOT_FOUND, "Product not found"));
        }
        var existingProduct = product.get();
        existingProduct.publish();
        outboxService.saveProductPublished(existingProduct);

        log.info("Published product {}", existingProduct.getId());
        return Result.success(DtoMapper.toDto(existingProduct));
    }

    @CacheEvict(value = "products", key = "#id")
    @Transactional
    public Result<ProductDto> takeDownProduct(UUID id) {
        var product = productRepository.findById(id);
        if (product.isEmpty()) {
            return Result.failure(new Error(DomainErrorCode.NOT_FOUND, "Product not found"));
        }
        var existingProduct = product.get();
        existingProduct.takeDown();
        outboxService.saveProductTakenDown(existingProduct);

        log.info("Took down product {}", existingProduct.getId());
        return Result.success(DtoMapper.toDto(existingProduct));
    }

    @CacheEvict(value = "products", key = "#id")
    @Transactional
    public Result<ProductDto> deleteProduct(UUID id) {
        // Trade-off: I didn't want to allow hard delete or enable full soft-delete for this assignment.
        return takeDownProduct(id);
    }

    @CacheEvict(value = "products", key = "#productId")
    @Transactional
    public Result<ProductDto> addTrack(UUID productId, AddTrackRequest request) {
        var product = productRepository.findById(productId);
        if (product.isEmpty()) {
            return Result.failure(new Error(DomainErrorCode.NOT_FOUND, "Product not found"));
        }

        var existingProduct = product.get();
        existingProduct.addTrack(
                request.title(),
                request.durationMs(),
                request.trackNumber(),
                request.isrc()
        );

        if (existingProduct.isPublished()) {
            outboxService.saveTrackListUpdated(existingProduct);
        }

        log.info("Added a track to product {}", existingProduct.getId());
        return Result.success(DtoMapper.toDto(existingProduct));
    }

    @CacheEvict(value = "products", key = "#productId")
    @Transactional
    public Result<ProductDto> removeTrack(UUID productId, UUID trackId) {
        var product = productRepository.findById(productId);
        if (product.isEmpty()) {
            return Result.failure(new Error(DomainErrorCode.NOT_FOUND, "Product not found"));
        }
        var existingProduct = product.get();
        var wasPublished = existingProduct.isPublished();

        existingProduct.removeTrack(trackId);

        if (wasPublished) {
            outboxService.saveTrackListUpdated(existingProduct);
        }
        log.info("Removed track {} from product {}", trackId, productId);
        return Result.success(DtoMapper.toDto(existingProduct));
    }

    @CacheEvict(value = "products", key = "#productId")
    @Transactional
    public Result<ProductDto> reorderTracks(UUID productId, ReorderTracksRequest request) {
        var product = productRepository.findById(productId);
        if (product.isEmpty()) {
            return Result.failure(new Error(DomainErrorCode.NOT_FOUND, "Product not found"));
        }
        var existingProduct = product.get();
        existingProduct.reorderTracks(request.trackOrder());
        if (existingProduct.isPublished()) {
            outboxService.saveTrackListUpdated(existingProduct);
        }
        log.info("Updated track order for product {}", productId);
        return Result.success(DtoMapper.toDto(existingProduct));
    }

    private Money toMoney(MoneyDto dto) {
        if (dto == null) return null;
        return Money.of(dto.amount(), dto.currency());
    }
}
