package com.clinic.identity_service.service;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.clinic.identity_service.dto.AccountData;
import com.clinic.identity_service.dto.AdminRegisterRequest;
import com.clinic.identity_service.dto.DoctorRegisterRequest;
import com.clinic.identity_service.dto.LoginRequest;
import com.clinic.identity_service.dto.LoginResponse;
import com.clinic.identity_service.dto.PatientRegisterRequest;
import com.clinic.identity_service.entity.Admin;
import com.clinic.identity_service.entity.Doctor;
import com.clinic.identity_service.entity.Patient;
import com.clinic.identity_service.entity.Role;
import com.clinic.identity_service.entity.Specialty;
import com.clinic.identity_service.exception.ExistedByMailException;
import com.clinic.identity_service.model.AppRole;
import com.clinic.identity_service.repository.AdminRepository;
import com.clinic.identity_service.repository.DoctorRepository;
import com.clinic.identity_service.repository.PatientRepository;

@Service
public class AuthService {

    private final Keycloak keycloak;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final AdminRepository adminRepository;
    private final EntityManager entityManager;
    private final String realm;
    private final String keycloakServerUrl;
    private final String authClientId;
    private final String authClientSecret;

    public AuthService(
        Keycloak keycloak,
        DoctorRepository doctorRepository,
        PatientRepository patientRepository,
        AdminRepository adminRepository,
        EntityManager entityManager,
        @Value("${keycloak.realm}") String realm,
        @Value("${keycloak.server-url}") String keycloakServerUrl,
        @Value("${keycloak.admin-client-id}") String authClientId,
        @Value("${keycloak.admin-client-secret}") String authClientSecret
    ) {
        this.keycloak = keycloak;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.adminRepository = adminRepository;
        this.entityManager = entityManager;
        this.realm = realm;
        this.keycloakServerUrl = keycloakServerUrl;
        this.authClientId = authClientId;
        this.authClientSecret = authClientSecret;
    }

    // NOTE: no @Transactional here. Keycloak cannot join a DB transaction,
    // so createAndSave() undoes the Keycloak user manually if the DB save fails.

    public UUID registerPatient(PatientRegisterRequest req) {
        AccountData account = req.account();
        return createAndSave(account, AppRole.PATIENT, id -> {
            Patient patient = new Patient();
            patient.setUserId(id);
            patient.setFirstName(account.firstName());
            patient.setLastName(account.lastName());
            patient.setEmail(account.email());
            patient.setDateOfBirth(account.dateOfBirth());
            patient.setRole(Role.PATIENT);
            patient.setIsActive(true);
            patient.setPhoneNumber(req.phoneNumber());
            patient.setAddress(req.address());
            patientRepository.saveAndFlush(patient);
        });
    }

    public UUID createDoctor(DoctorRegisterRequest req) {
        // Validate everything that can be checked BEFORE touching Keycloak
        Specialty specialty = entityManager.find(Specialty.class, req.specialtyId());
        if (specialty == null) {
            throw new EntityNotFoundException("Specialty not found: " + req.specialtyId());
        }

        AccountData account = req.account();
        return createAndSave(account, AppRole.DOCTOR, id -> {
            Doctor doctor = new Doctor();
            doctor.setUserId(id);
            doctor.setFirstName(account.firstName());
            doctor.setLastName(account.lastName());
            doctor.setEmail(account.email());
            doctor.setDateOfBirth(account.dateOfBirth());
            doctor.setRole(Role.DOCTOR);
            doctor.setIsActive(true);
            doctor.setPhoneNumber(req.phoneNumber());
            doctor.setAddress(req.address());
            doctor.setLocalisation(req.localisation());
            doctor.setSpecialty(specialty);
            doctorRepository.saveAndFlush(doctor);
        });
    }

    public UUID createAdmin(AdminRegisterRequest req) {
        AccountData account = req.account();
        return createAndSave(account, AppRole.ADMIN, id -> {
            Admin admin = new Admin();
            admin.setUserId(id);
            admin.setFirstName(account.firstName());
            admin.setLastName(account.lastName());
            admin.setEmail(account.email());
            admin.setDateOfBirth(account.dateOfBirth());
            admin.setRole(Role.ADMIN);
            admin.setIsActive(true);
            adminRepository.saveAndFlush(admin);
        });
    }

    public LoginResponse login(LoginRequest req) {

    try (Keycloak userKeycloak = KeycloakBuilder.builder()
            .serverUrl(keycloakServerUrl)
            .realm(realm)
            .clientId(authClientId)
            .clientSecret(authClientSecret)
            .username(req.email())
            .password(req.password())
            .grantType(OAuth2Constants.PASSWORD)
            .build()) {

        AccessTokenResponse token =
                userKeycloak.tokenManager().getAccessToken();

        return new LoginResponse(
                token.getToken(),
                token.getRefreshToken(),
                (long) token.getExpiresIn(),
                (long) token.getRefreshExpiresIn(),
                token.getTokenType()
        );

    } catch (NotAuthorizedException | BadRequestException e) {
        throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid email or password"
        );
    }
}

    /**
     * Creates the Keycloak user, then runs the local persistence step.
     * If anything in the persistence step fails, the Keycloak user is deleted
     * so no orphaned account is left behind.
     */
    private UUID createAndSave(AccountData account, AppRole role, Consumer<UUID> persist) {
        String keycloakId = createKeycloakUser(account, role);
        UUID id = UUID.fromString(keycloakId);
        try {
            persist.accept(id);
            return id;
        } catch (RuntimeException e) {
            try {
                keycloak.realm(realm).users().delete(keycloakId).close();
            } catch (RuntimeException cleanupError) {
                e.addSuppressed(cleanupError); // keep the original error visible
            }
            throw e;
        }
    }

    private String createKeycloakUser(AccountData account, AppRole role) {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(account.password());
        credential.setTemporary(false);

        UserRepresentation user = new UserRepresentation();
        user.setUsername(account.email());
        user.setEmail(account.email());
        user.setFirstName(account.firstName());
        user.setLastName(account.lastName());
        user.setEnabled(true);
        user.setCredentials(List.of(credential));

        String userId;
        try (Response response = keycloak.realm(realm).users().create(user)) {
            if (response.getStatus() == 409) {
                throw new ExistedByMailException("User with this email already exists.");
            }
            if (response.getStatus() != 201) {
                throw new IllegalStateException(
                        "Keycloak user creation failed: HTTP " + response.getStatus());
            }
            userId = CreatedResponseUtil.getCreatedId(response);
        }

        try {
            RoleRepresentation roleRep = keycloak.realm(realm)
                    .roles().get(role.keycloakName()).toRepresentation();
            keycloak.realm(realm).users().get(userId)
                    .roles().realmLevel().add(List.of(roleRep));
        } catch (RuntimeException e) {
            try {
                keycloak.realm(realm).users().delete(userId).close();
            } catch (RuntimeException cleanupError) {
                e.addSuppressed(cleanupError);
            }
            throw e;
        }
        return userId;
    }
}