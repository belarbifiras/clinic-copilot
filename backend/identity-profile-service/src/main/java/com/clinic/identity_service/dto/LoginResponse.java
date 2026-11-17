package com.clinic.identity_service.dto;

public record LoginResponse(
    String accessToken,
    String refreshToken,
    Long expiresIn,
    Long refreshExpiresIn,
    String tokenType
) {}
