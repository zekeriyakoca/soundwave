package com.soundwave.api.service;

import com.soundwave.api.contract.DtoMapper;
import com.soundwave.api.contract.response.ArtistDto;
import com.soundwave.api.contract.response.PagedResponse;
import com.soundwave.api.contract.request.CreateArtistRequest;
import com.soundwave.api.contract.request.UpdateArtistRequest;
import com.soundwave.domain.dto.DomainErrorCode;
import com.soundwave.domain.dto.Error;
import com.soundwave.domain.dto.Result;
import com.soundwave.domain.entity.Artist;
import com.soundwave.infrastructure.persistence.repository.ArtistRepository;
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
public class ArtistService {

    private final ArtistRepository artistRepository;
    private final OutboxService outboxService;

    @Transactional
    public Result<ArtistDto> createArtist(CreateArtistRequest request) {
        var artist = Artist.create(request.name(), request.bio());
        var saved = artistRepository.save(artist);
        log.info("Created artist {}", saved.getId());
        return Result.success(DtoMapper.toDto(saved));
    }

    @Cacheable(value = "artists", key = "#id")
    @Transactional(readOnly = true)
    public Result<ArtistDto> getArtist(UUID id) {
        return artistRepository.findById(id)
                .map(artist -> Result.success(DtoMapper.toDto(artist)))
                .orElseGet(() -> Result.failure(
                        new Error(DomainErrorCode.NOT_FOUND, "Artist not found")
                ));
    }

    @Transactional(readOnly = true)
    public Result<PagedResponse<ArtistDto>> getAllArtists(Pageable page) {
        var artists = artistRepository.findAll(page);
        var response = new PagedResponse<>(
                artists.map(DtoMapper::toDto).getContent(),
                artists.getNumber(),
                artists.getSize(),
                artists.getTotalElements(),
                artists.getTotalPages()
        );
        return Result.success(response);
    }

    @CacheEvict(value = "artists", key = "#id")
    @Transactional
    public Result<ArtistDto> updateArtist(UUID id, UpdateArtistRequest request) {
        var artist = artistRepository.findById(id);
        if (artist.isEmpty()) {
            return Result.failure(new Error(DomainErrorCode.NOT_FOUND, "Artist not found"));
        }
        var existingArtist = artist.get();
        var changed = existingArtist.updateProfile(request.name(), request.bio());
        if (changed) {
            outboxService.saveArtistUpdated(existingArtist);
            log.info("Updated artist {}", existingArtist.getId());
        }
        return Result.success(DtoMapper.toDto(existingArtist));
    }

}
