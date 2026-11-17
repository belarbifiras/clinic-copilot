package com.clinic.identity_service.model;

public enum AppRole {
    PATIENT("patient"),
    DOCTOR("doctor"),
    ADMIN("admin");

    private final String keycloakName;

    AppRole(String keycloakName) {
        this.keycloakName = keycloakName;
    }

    public String keycloakName() {
        return keycloakName;
    }
}