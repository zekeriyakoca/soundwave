package com.soundwave.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "tracks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Track {

    @Id
    @Column(columnDefinition = "CHAR(36)")
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private Integer durationMs;

    @Column(nullable = false)
    private Integer trackNumber;

    @Column(length = 12)
    private String isrc;

    Track(UUID id, String title, int durationMs, int trackNumber, String isrc) {
        this.id = Objects.requireNonNull(id, "Track id cannot be null");
        setTitle(title);
        setDurationMs(durationMs);
        setTrackNumber(trackNumber);
        setIsrc(isrc);
    }

    void reorder(int newTrackNumber) {
        setTrackNumber(newTrackNumber);
    }

    private void setTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Track title cannot be blank");
        }
        if (title.strip().length() > 255) {
            throw new IllegalArgumentException("Track title cannot exceed 255 characters");
        }
        this.title = title.strip();
    }

    private void setIsrc(String isrc) {
        if (isrc != null && isrc.length() > 12) {
            throw new IllegalArgumentException("ISRC cannot exceed 12 characters");
        }
        this.isrc = isrc;
    }

    private void setDurationMs(int durationMs) {
        if (durationMs <= 0) {
            throw new IllegalArgumentException("Track duration must be positive");
        }
        this.durationMs = durationMs;
    }

    private void setTrackNumber(int trackNumber) {
        if (trackNumber <= 0) {
            throw new IllegalArgumentException("Track number must be positive");
        }
        this.trackNumber = trackNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Track other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
