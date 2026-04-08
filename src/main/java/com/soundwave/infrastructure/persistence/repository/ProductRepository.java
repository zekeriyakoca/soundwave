package com.soundwave.infrastructure.persistence.repository;

import com.soundwave.domain.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    @EntityGraph(attributePaths = {"artist", "tracks"})
    Optional<Product> findWithDetailsById(UUID id);

    @EntityGraph(attributePaths = {"artist"})
    Page<Product> findAll(Specification<Product> spec, Pageable pageable);
}
