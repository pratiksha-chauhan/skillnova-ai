package com.skillnova.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Persistence model for a registered SkillNova AI user.
 *
 * Mirrors the "users" table created by
 * {@code db/migration/V1__create_core_schema.sql}. Profile fields
 * (current/target role, skills, study hours, timeline) are captured directly
 * on the user, since the schema has no separate profile table; they are
 * populated during registration and later updated via profile management
 * (Phase 7) and read when generating roadmaps (Phase 8).
 */
@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "uq_users_email", columnNames = "email"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "current_job_role", length = 150)
    private String currentJobRole;

    @Column(name = "years_experience", precision = 4, scale = 1)
    private BigDecimal yearsExperience;

    @Column(name = "current_skills", columnDefinition = "TEXT")
    private String currentSkills;

    @Column(name = "target_role", length = 150)
    private String targetRole;

    @Column(name = "daily_study_hours", precision = 4, scale = 1)
    private BigDecimal dailyStudyHours;

    @Column(name = "target_timeline_months")
    private Integer targetTimelineMonths;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
