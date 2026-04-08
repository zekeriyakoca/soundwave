package com.soundwave.api.controller;

import com.soundwave.api.contract.ResultResponseMapper;
import com.soundwave.api.contract.request.AddTrackRequest;
import com.soundwave.api.contract.request.CreateProductRequest;
import com.soundwave.api.contract.request.ReassignArtistRequest;
import com.soundwave.api.contract.request.ReorderTracksRequest;
import com.soundwave.api.contract.request.UpdateProductMetadataRequest;
import com.soundwave.api.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable UUID id) {
        return ResultResponseMapper.toResponse(productService.getProduct(id));
    }

    @GetMapping
    public ResponseEntity<?> list(
            @PageableDefault(size = 20, sort = "title") Pageable page,
            @RequestParam(required = false) UUID artistId) {
        return ResultResponseMapper.toResponse(productService.searchProducts(page, artistId));
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreateProductRequest request) {
        return ResultResponseMapper.toResponse(productService.createProductAsDraft(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @Valid @RequestBody UpdateProductMetadataRequest request) {
        return ResultResponseMapper.toResponse(productService.updateProductMetadata(id, request));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<?> publish(@PathVariable UUID id) {
        return ResultResponseMapper.toResponse(productService.publishProduct(id));
    }

    @PostMapping("/{id}/takedown")
    public ResponseEntity<?> takedown(@PathVariable UUID id) {
        return ResultResponseMapper.toResponse(productService.takeDownProduct(id));
    }

    @PutMapping("/{id}/artist")
    public ResponseEntity<?> reassignArtist(@PathVariable UUID id, @Valid @RequestBody ReassignArtistRequest request) {
        return ResultResponseMapper.toResponse(productService.reassignArtist(id, request));
    }

    @GetMapping("/{id}/tracks")
    public ResponseEntity<?> listTracks(@PathVariable UUID id) {
        return ResultResponseMapper.toResponse(productService.getProductTracks(id));
    }

    @PostMapping("/{id}/tracks")
    public ResponseEntity<?> addTrack(@PathVariable UUID id, @Valid @RequestBody AddTrackRequest request) {
        return ResultResponseMapper.toResponse(productService.addTrack(id, request), HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}/tracks/{trackId}")
    public ResponseEntity<?> removeTrack(@PathVariable UUID id, @PathVariable UUID trackId) {
        return ResultResponseMapper.toResponse(productService.removeTrack(id, trackId));
    }

    @PutMapping("/{id}/tracks/order")
    public ResponseEntity<?> reorderTracks(@PathVariable UUID id, @Valid @RequestBody ReorderTracksRequest request) {
        return ResultResponseMapper.toResponse(productService.reorderTracks(id, request));
    }
}
