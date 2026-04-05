package com.soundwave.api.controller;

import com.soundwave.api.contract.request.CreateArtistRequest;
import com.soundwave.api.contract.request.UpdateArtistRequest;
import com.soundwave.api.contract.ResultResponseMapper;
import com.soundwave.api.service.ArtistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/artists")
@RequiredArgsConstructor
public class ArtistController {

    private final ArtistService artistService;

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable UUID id) {
        return ResultResponseMapper.toResponse(artistService.getArtist(id));
    }

    // Trade-off: I left search out for this assignment.
    // Doing it properly needs clear query strategy and index design decisions.
    @GetMapping
    public ResponseEntity<?> getAll(
            @PageableDefault(size = 20, sort = "name") Pageable page) {
        return ResultResponseMapper.toResponse(artistService.getAllArtists(page));
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreateArtistRequest request) {
        return ResultResponseMapper.toResponse(artistService.createArtist(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @Valid @RequestBody UpdateArtistRequest request) {
        return ResultResponseMapper.toResponse(artistService.updateArtist(id, request));
    }
}
