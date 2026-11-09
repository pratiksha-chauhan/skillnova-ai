package com.skillnova.backend.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Persistence model for a single AI-generated roadmap (Phase 9). Mirrors
 * the "roadmaps" table created by
 * {@code db/migration/V1__create_core_schema.sql}, with
 * {@link RoadmapTask}, {@link SkillGap}, {@link CertificationRecommendation}
 * and {@link ProjectRecommendation} as cascade-managed child collections -
 * saving a {@code Roadmap} persists its entire object graph in one call,
 * and deleting it cascades (both at the JPA and the database FK level) to
 * every child row.
 *
 * {@code rawAiResponse} retains the raw JSON returned by the AI provider
 * for debugging/audit purposes, separate from the structured child rows
 * used by the rest of the application.
 *
 * Built with a plain constructor + setters (no Lombok {@code @Builder} on
 * the collections) so the {@code add*} helpers below are always used to
 * keep both sides of each bidirectional association in sync.
 */
@Entity
@Table(name = "roadmaps")
@Getter
@Setter
@NoArgsConstructor
public class Roadmap {

    @Id
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Column(name = "target_role", nullable = false, length = 150)
    private String targetRole;

    @Column(name = "current_job_role", length = 150)
    private String currentJobRole;

    @Column(name = "years_experience", precision = 4, scale = 1)
    private BigDecimal yearsExperience;

    @Column(name = "daily_study_hours", precision = 4, scale = 1)
    private BigDecimal dailyStudyHours;

    @Column(name = "target_timeline_months")
    private Integer targetTimelineMonths;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RoadmapStatus status = RoadmapStatus.ACTIVE;

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    @Column(name = "raw_ai_response", columnDefinition = "TEXT")
    private String rawAiResponse;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "roadmap", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("monthNumber ASC, sortOrder ASC")
    private List<RoadmapTask> tasks = new ArrayList<>();

    @OneToMany(mappedBy = "roadmap", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<SkillGap> skillGaps = new ArrayList<>();

    @OneToMany(mappedBy = "roadmap", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CertificationRecommendation> certifications = new ArrayList<>();

    @OneToMany(mappedBy = "roadmap", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ProjectRecommendation> projects = new ArrayList<>();

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

    public void addTask(RoadmapTask task) {
        task.setRoadmap(this);
        tasks.add(task);
    }

    public void addSkillGap(SkillGap skillGap) {
        skillGap.setRoadmap(this);
        skillGaps.add(skillGap);
    }

    public void addCertification(CertificationRecommendation certification) {
        certification.setRoadmap(this);
        certifications.add(certification);
    }

    public void addProject(ProjectRecommendation project) {
        project.setRoadmap(this);
        projects.add(project);
    }
}
