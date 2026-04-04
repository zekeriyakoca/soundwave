package com.soundwave.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.Getter;

@MappedSuperclass
@Getter
public abstract class VersionedEntity {

    @Version
    @Column(nullable = false)
    private long version;
}
