package com.skillnova.backend.dto;

import java.util.UUID;

/**
 * Returned on successful login. No access token is issued yet -
 * JWT issuance and the "Authorization: Bearer" contract are introduced in
 * Phase 6. For now this confirms the credentials were valid and returns the
 * identity the frontend needs to proceed.
 */
public record LoginResponse(
        UUID id,
        String email,
        String fullName
) {}
