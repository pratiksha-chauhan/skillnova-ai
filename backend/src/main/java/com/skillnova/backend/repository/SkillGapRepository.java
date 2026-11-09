package com.skillnova.backend.repository;

import com.skillnova.backend.entity.RoadmapStatus;
import com.skillnova.backend.entity.SkillGap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SkillGapRepository extends JpaRepository<SkillGap, UUID> {

    /**
     * Skill-gap counts grouped by priority across the user's roadmaps in
     * the given status (the dashboard only cares about gaps on currently
     * ACTIVE roadmaps). Each row is {@code [Priority priority, Long count]}.
     */
    @Query("""
            select s.priority, count(s)
            from SkillGap s
            where s.roadmap.user.id = :userId and s.roadmap.status = :roadmapStatus
            group by s.priority
            """)
    List<Object[]> tallyPriorityCountsByUserAndRoadmapStatus(
            @Param("userId") UUID userId, @Param("roadmapStatus") RoadmapStatus roadmapStatus);
}
