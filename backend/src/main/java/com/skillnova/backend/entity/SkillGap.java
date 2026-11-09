package com.skillnova.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Persistence model for a single identified skill gap. Mirrors the
 * "skill_gaps" table created by {@code db/migration/V1__create_core_schema.sql}.
 * Immutable once created (no {@code updated_at} column) - skill gaps are a
 * point-in-time snapshot taken when the roadmap was generated.
 */
@Entity
@Table(name = "skill_gaps")
@Getter
@Setter
@NoArgsConstructor
public class SkillGap {

    @Id
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "roadmap_id", nullable = false, updatable = false)
    private Roadmap roadmap;

    @Column(name = "skill_name", nullable = false, length = 150)
    private String skillName;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_level", nullable = false, length = 30)
    private SkillLevel currentLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "required_level", nullable = false, length = 30)
    private SkillLevel requiredLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Priority priority = Priority.MEDIUM;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = Instant.now();
    }
}
