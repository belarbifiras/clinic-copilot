package com.clinic.identity_service.controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.clinic.identity_service.service.UserService;

/**
 * Endpoints internes, accessibles uniquement par les autres microservices
 * via la clé partagée X-Internal-Api-Key.
 * Ces endpoints contournent la vérification JWT (voir SecurityConfiguration).
 */
@RestController
@RequestMapping("/internal/users")
public class InternalUserController {

    private final UserService userService;
    private final String internalApiKey;

    public InternalUserController(UserService userService,
            @Value("${app.internal.api-key:changeme-internal-key}") String internalApiKey) {
        this.userService = userService;
        this.internalApiKey = internalApiKey;
    }

    @PatchMapping("/{userId}/activate")
    public ResponseEntity<Void> activate(
            @PathVariable UUID userId,
            @RequestHeader("X-Internal-Api-Key") String key) {
        if (!internalApiKey.equals(key)) {
            return ResponseEntity.status(403).build();
        }
        userService.activateUser(userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{userId}/deactivate")
    public ResponseEntity<Void> deactivate(
            @PathVariable UUID userId,
            @RequestHeader("X-Internal-Api-Key") String key) {
        if (!internalApiKey.equals(key)) {
            return ResponseEntity.status(403).build();
        }
        userService.deactivateUser(userId);
        return ResponseEntity.noContent().build();
    }
}
