package com.clinic.identity_service.service;

import jakarta.ws.rs.core.Response;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.clinic.identity_service.entity.Role;
import com.clinic.identity_service.exception.ExistedByMailException;

import java.util.List;
import java.util.UUID;

@Service
public class KeycloakAdminService {

    private final Keycloak keycloak;
    private final String realm;

    public KeycloakAdminService(Keycloak keycloak, @Value("${keycloak.realm}") String realm) {
        this.keycloak = keycloak;
        this.realm = realm;
    }

    /** Creates the user in Keycloak and returns its id (the future "sub" in the token). */
    public UUID createUser(String firstName, String lastName, String email,
                           String password, Role role) {

        UserRepresentation user = new UserRepresentation();
        user.setUsername(email);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEnabled(true);
        user.setEmailVerified(true);

        CredentialRepresentation cred = new CredentialRepresentation();
        cred.setType(CredentialRepresentation.PASSWORD);
        cred.setValue(password);
        cred.setTemporary(false);
        user.setCredentials(List.of(cred));

        try (Response response = keycloak.realm(realm).users().create(user)) {
            if (response.getStatus() == 409) {
                throw new ExistedByMailException("User with this email already exists.");
            }
            if (response.getStatus() != 201) {
                throw new IllegalStateException("Keycloak user creation failed: " + response.getStatus());
            }

            String path = response.getLocation().getPath();
            String id = path.substring(path.lastIndexOf('/') + 1);

            var roleRep = keycloak.realm(realm).roles().get(role.name()).toRepresentation();
            keycloak.realm(realm).users().get(id).roles().realmLevel().add(List.of(roleRep));

            return UUID.fromString(id);
        }
    }

    public void setEnabled(UUID userId, boolean enabled) {
        var resource = keycloak.realm(realm).users().get(userId.toString());
        UserRepresentation rep = resource.toRepresentation();
        rep.setEnabled(enabled);
        resource.update(rep);
    }

    public void deleteUser(UUID userId) {
        keycloak.realm(realm).users().get(userId.toString()).remove();
    }
}