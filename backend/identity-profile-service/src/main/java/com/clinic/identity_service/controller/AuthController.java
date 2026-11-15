package com.clinic.identity_service.controller;

import com.clinic.identity_service.dto.RegisterRequest;
import com.clinic.identity_service.dto.UserResponse;
import com.clinic.identity_service.entity.User;
import com.clinic.identity_service.service.AuthService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {

        User user = authService.registerUser(request);

        UserResponse response = new UserResponse(
            user.getUserId(),
            user.getFirstName(),
            user.getLastName(),
            user.getEmail(),
            user.getDateOfBirth(),
            user.getCreatedAt(),
            user.getRole(),
            user.getIsActive()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}