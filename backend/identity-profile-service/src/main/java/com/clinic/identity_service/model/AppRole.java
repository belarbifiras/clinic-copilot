
package com.clinic.identity_service.model;

public enum AppRole {
    PATIENT("PATIENT"),
    DOCTOR("DOCTOR"),
    ADMIN("ADMIN");

    private final String keycloakName;

    AppRole(String keycloakName) {
        this.keycloakName = keycloakName;
    }

    public String keycloakName() {
        return keycloakName;
    }
}
