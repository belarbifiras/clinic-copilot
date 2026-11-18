package com.clinic.identity_service.dto;

import java.util.UUID;

public record SpecialtyResponse(
    UUID specialtyId, 
    String name
) {}