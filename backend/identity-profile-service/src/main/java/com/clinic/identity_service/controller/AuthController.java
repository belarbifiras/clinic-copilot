package com.clinic.identity_service.controller;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.clinic.identity_service.dto.AdminRegisterRequest;
import com.clinic.identity_service.dto.DoctorRegisterRequest;
import com.clinic.identity_service.dto.LoginRequest;
import com.clinic.identity_service.dto.LoginResponse;
import com.clinic.identity_service.dto.PatientRegisterRequest;
import com.clinic.identity_service.service.AuthService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register-as-a-patient")
    public ResponseEntity<Map<String, UUID>> registerPatient(
        @Valid @RequestBody PatientRegisterRequest request
    ) {
        UUID id = authService.registerPatient(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", id));
    }

    @PostMapping("/resgister-as-a-doctor")
    public ResponseEntity<Map<String, UUID>> registerDoctor(
        @Valid @RequestBody DoctorRegisterRequest request
    ) {
        UUID id = authService.createDoctor(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", id));
    }

    @PostMapping("/create-admin")
    public ResponseEntity<Map<String, UUID>> createAdmin(
        @Valid @RequestBody AdminRegisterRequest request
    ) {
        UUID id = authService.createAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", id));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }


}
