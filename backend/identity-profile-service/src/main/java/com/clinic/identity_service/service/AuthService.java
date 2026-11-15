package com.clinic.identity_service.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.clinic.identity_service.dto.RegisterRequest;
import com.clinic.identity_service.entity.Role;
import com.clinic.identity_service.entity.User;
import com.clinic.identity_service.exception.ExistedByMailException;
import com.clinic.identity_service.exception.RoleNotAllowedException;
import com.clinic.identity_service.repository.UserRepository;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final KeycloakAdminService keycloakAdminService;

    public AuthService(UserRepository userRepository, KeycloakAdminService keycloakAdminService) {
        this.userRepository = userRepository;
        this.keycloakAdminService = keycloakAdminService;
    }

    public User registerUser(RegisterRequest request) {

        // Keycloak lowercases emails, so normalize here to keep both stores identical.
        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new ExistedByMailException("User with this email already exists.");
        }

        Role role = request.role() == null ? Role.PATIENT : request.role();

        // Anyone can sign up, but only as a patient or a doctor.
        if (role == Role.ADMIN) {
            throw new RoleNotAllowedException("This role cannot be chosen at registration.");
        }

        UUID keycloakId = keycloakAdminService.createUser(
                request.firstName(), request.lastName(),
                email, request.password(), role);

        try {
            User user = new User();
            user.setUserId(keycloakId);
            user.setFirstName(request.firstName());
            user.setLastName(request.lastName());
            user.setEmail(email);
            user.setRole(role);
            user.setIsActive(true);
            user.setDateOfBirth(request.dateOfBirth());
            return userRepository.save(user);
        } catch (RuntimeException e) {
            keycloakAdminService.deleteUser(keycloakId); // avoid an orphan account in Keycloak
            throw e;
        }
    }
}