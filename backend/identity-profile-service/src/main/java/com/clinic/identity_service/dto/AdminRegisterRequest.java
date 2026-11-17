package com.clinic.identity_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record AdminRegisterRequest(
    @Valid
    @NotNull(message = "Account data is required")
    AccountData account
) {}
