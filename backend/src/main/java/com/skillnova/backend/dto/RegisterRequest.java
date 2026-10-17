package com.skillnova.backend.dto;

public record RegisterRequest(
        String name,
        String email,
        String password
) {}
