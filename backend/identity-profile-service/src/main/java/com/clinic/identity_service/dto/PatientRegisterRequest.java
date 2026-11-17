package com.clinic.identity_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PatientRegisterRequest(

    @Valid
    @NotNull(message = "Account data is required")
    AccountData account,

    @NotBlank(message = "phone number is required")
    @Size(max = 30, message = "Phone number must be at most 30 characters")
    String phoneNumber,

    @Size(max = 100, message = "Address must be at most 100 characters")
    String address
) {}
