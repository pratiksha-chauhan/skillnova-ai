package com.skillnova.backend.controller;

import com.skillnova.backend.dto.RoadmapAiResponse;
import com.skillnova.backend.dto.RoadmapGenerationRequest;
import com.skillnova.backend.entity.User;
import com.skillnova.backend.service.RoadmapService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI-powered roadmap generation (Phase 8 - preview only, not persisted).
 * Requires a valid JWT ({@code JwtAuthenticationFilter} populates
 * {@code @AuthenticationPrincipal User}).
 */
@RestController
@RequestMapping("/api/roadmap")
public class RoadmapController {

    private final RoadmapService roadmapService;

    public RoadmapController(RoadmapService roadmapService) {
        this.roadmapService = roadmapService;
    }

    /**
     * Request body is optional - omit it entirely to generate purely from
     * the stored profile, or include any subset of fields to override it
     * for this one generation without changing the stored profile.
     */
    @PostMapping("/generate")
    public ResponseEntity<RoadmapAiResponse> generate(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody(required = false) RoadmapGenerationRequest request
    ) {
        RoadmapGenerationRequest effectiveRequest = request != null ? request : RoadmapGenerationRequest.EMPTY;
        RoadmapAiResponse response = roadmapService.generateRoadmap(user.getId(), effectiveRequest);
        return ResponseEntity.ok(response);
    }
}
