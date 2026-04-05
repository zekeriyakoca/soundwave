package com.soundwave.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Formula;

import java.time.LocalDate;
import java.util.*;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseEntity {

    @Id
    @Column(columnDefinition = "CHAR(36)")
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(unique = true, length = 12)
    private String upc;

    private LocalDate releaseDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Genre genre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductStatus status;

    @Embedded
    private Money price;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id", nullable = false)
    private Artist artist;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @OrderBy("trackNumber ASC")
    private List<Track> tracks = new ArrayList<>();

    @Formula("(SELECT COUNT(*) FROM tracks t WHERE t.product_id = id)")
    private int trackCount;

    private Product(UUID id, String title, String upc, LocalDate releaseDate,
                    Genre genre, Money price, Artist artist) {
        this.id = Objects.requireNonNull(id, "Product id cannot be null");
        this.artist = Objects.requireNonNull(artist, "Artist cannot be null");
        this.status = ProductStatus.DRAFT;
        setTitle(title);
        setUpc(upc);
        this.releaseDate = releaseDate;
        setGenre(genre);
        this.price = price;
    }

    public static Product create(String title, String upc, LocalDate releaseDate,
                                  Genre genre, Money price, Artist artist) {
        return new Product(UUID.randomUUID(), title, upc, releaseDate, genre, price, artist);
    }

    public void publish() {
        if (status != ProductStatus.DRAFT && status != ProductStatus.TAKEN_DOWN) {
            throw new IllegalStateException("Product must be in DRAFT or TAKEN_DOWN status to publish");
        }
        if (tracks.isEmpty()) {
            throw new IllegalStateException("Cannot publish product without tracks");
        }
        this.status = ProductStatus.PUBLISHED;
    }

    public void takeDown() {
        if (status != ProductStatus.PUBLISHED) {
            throw new IllegalStateException("Product must be in PUBLISHED status to take down");
        }
        this.status = ProductStatus.TAKEN_DOWN;
    }

    public boolean updateMetadata(String title, String upc, LocalDate releaseDate,
                                  Genre genre, Money price) {
        var changed =
                !Objects.equals(this.title, normalizeTitle(title)) ||
                !Objects.equals(this.upc, normalizeUpc(upc)) ||
                !Objects.equals(this.releaseDate, releaseDate) ||
                this.genre != genre ||
                !Objects.equals(this.price, price);

        setTitle(title);
        setUpc(upc);
        this.releaseDate = releaseDate;
        setGenre(genre);
        this.price = price;
        return changed;
    }

    public boolean isPublished() {
        return status == ProductStatus.PUBLISHED;
    }

    public void addTrack(String title, int durationMs, int trackNumber, String isrc) {
        var numberTaken = tracks.stream()
                .anyMatch(t -> t.getTrackNumber() == trackNumber);
        if (numberTaken) {
            throw new IllegalStateException("Track number " + trackNumber + " already exists");
        }
        tracks.add(new Track(UUID.randomUUID(), title, durationMs, trackNumber, isrc));
    }

    public void removeTrack(UUID trackId) {
        var track = tracks.stream()
                .filter(t -> t.getId().equals(trackId))
                .findFirst()
                .orElse(null);

        if (track == null) {
            throw new IllegalArgumentException("Track not found: " + trackId);
        }

        if (isPublished() && tracks.size() <= 1) {
            throw new IllegalStateException("Published product must have at least one track");
        }

        tracks.remove(track);
    }

    public void reorderTracks(List<UUID> trackOrder) {
        if (trackOrder.size() != tracks.size()) {
            throw new IllegalArgumentException("Track order must contain all track ids");
        }
        if (new HashSet<>(trackOrder).size() != trackOrder.size()) {
            throw new IllegalArgumentException("Duplicate track ids in order");
        }
        var trackMap = new HashMap<UUID, Track>();
        for (var track : tracks) {
            trackMap.put(track.getId(), track);
        }
        for (var trackId : trackOrder) {
            if (!trackMap.containsKey(trackId)) {
                throw new IllegalArgumentException("Unknown track id: " + trackId);
            }
        }
        for (int i = 0; i < trackOrder.size(); i++) {
            trackMap.get(trackOrder.get(i)).reorder(i + 1);
        }
        tracks.sort(Comparator.comparing(Track::getTrackNumber));
    }

    private void setTitle(String title) {
        this.title = normalizeTitle(title);
    }

    private void setUpc(String upc) {
        this.upc = normalizeUpc(upc);
    }

    private void setGenre(Genre genre) {
        this.genre = Objects.requireNonNull(genre, "Genre cannot be null");
    }

    private static String normalizeTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Product title cannot be blank");
        }
        var normalized = title.strip();
        if (normalized.length() > 255) {
            throw new IllegalArgumentException("Product title cannot exceed 255 characters");
        }
        return normalized;
    }

    private static String normalizeUpc(String upc) {
        if (upc == null || upc.isBlank()) {
            return null;
        }
        var normalized = upc.strip();
        if (!normalized.matches("^\\d{12}$")) {
            throw new IllegalArgumentException("UPC must be exactly 12 digits");
        }
        return normalized;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Product other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
