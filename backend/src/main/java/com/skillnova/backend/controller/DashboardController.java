package com.skillnova.backend.controller;

import com.skillnova.backend.dto.DashboardResponse;
import com.skillnova.backend.entity.User;
import com.skillnova.backend.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Aggregated overview for the authenticated user's own roadmaps/tasks/skill
 * gaps (Phase 10). Requires a valid JWT, same as every other authenticated
 * route ({@code JwtAuthenticationFilter} populates
 * {@code @AuthenticationPrincipal User}).
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public ResponseEntity<DashboardResponse> getDashboard(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(dashboardService.getDashboard(user.getId()));
    }
}
