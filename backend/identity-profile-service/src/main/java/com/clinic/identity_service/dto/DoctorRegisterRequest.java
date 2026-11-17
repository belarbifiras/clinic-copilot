package com.clinic.identity_service.dto;

import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DoctorRegisterRequest(

    @Valid
    @NotNull(message = "Account data is required")
    AccountData account,

    @Size(max = 30, message = "Phone number must be at most 30 characters")
    String phoneNumber,

    @Size(max = 100, message = "Address must be at most 100 characters")
    String address,

    @Size(max = 100, message = "Localisation must be at most 100 characters")
    String localisation,

    @NotNull(message = "Specialty is required")
    UUID specialtyId
) {}
