package com.skillnova.backend.repository;

import com.skillnova.backend.entity.Roadmap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoadmapRepository extends JpaRepository<Roadmap, UUID> {

    List<Roadmap> findAllByUser_IdOrderByCreatedAtDesc(UUID userId);

    /**
     * Ownership-scoped lookup: returns empty (not just the roadmap) when the
     * roadmap exists but belongs to a different user, so callers can treat
     * "not found" and "not yours" identically (404, no 403 needed) without
     * leaking whether the ID exists at all.
     */
    Optional<Roadmap> findByIdAndUser_Id(UUID id, UUID userId);
}
