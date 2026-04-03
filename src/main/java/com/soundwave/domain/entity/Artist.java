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
@Table(name = "artists")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Artist extends BaseEntity {

    @Id
    @Column(columnDefinition = "CHAR(36)")
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String bio;

    private Artist(UUID id, String name, String bio) {
        this.id = Objects.requireNonNull(id, "Artist id cannot be null");
        setName(name);
        setBio(bio);
    }

    public static Artist create(String name, String bio) {
        return new Artist(UUID.randomUUID(), name, bio);
    }

    public void updateProfile(String name, String bio) {
        setName(name);
        setBio(bio);
    }

    private void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Artist name cannot be blank");
        }
        if (name.strip().length() > 255) {
            throw new IllegalArgumentException("Artist name cannot exceed 255 characters");
        }
        this.name = name.strip();
    }

    private void setBio(String bio) {
        this.bio = (bio == null || bio.isBlank()) ? null : bio.strip();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Artist other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
