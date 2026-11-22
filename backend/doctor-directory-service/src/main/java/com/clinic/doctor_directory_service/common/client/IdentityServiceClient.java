package com.clinic.doctor_directory_service.common.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Client REST interne vers l'Identity Service.
 * Utilisé pour activer/désactiver le compte Keycloak d'un médecin
 * lorsque son diplôme est validé ou rejeté par un administrateur.
 */
@Component
public class IdentityServiceClient {

    private static final Logger log = LoggerFactory.getLogger(IdentityServiceClient.class);

    private final RestClient restClient;
    private final String internalApiKey;

    public IdentityServiceClient(
            @Value("${app.clients.identity-service.base-url:http://localhost:8081}") String baseUrl,
            @Value("${app.clients.identity-service.internal-api-key:changeme-internal-key}") String internalApiKey) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.internalApiKey = internalApiKey;
    }

    /**
     * Active le compte Keycloak d'un utilisateur (permet la connexion).
     * Appelé quand un diplôme de médecin est validé par l'admin.
     */
    public void activateUser(String userId) {
        try {
            restClient.patch()
                    .uri("/internal/users/{userId}/activate", userId)
                    .header("X-Internal-Api-Key", internalApiKey)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Compte activé dans Identity pour userId={}", userId);
        } catch (RestClientException e) {
            // On log l'erreur sans bloquer la validation du diplôme.
            // L'admin pourra activer manuellement depuis la page utilisateurs.
            log.error("Impossible d'activer le compte Identity pour userId={} : {}", userId, e.getMessage());
        }
    }

    /**
     * Désactive le compte Keycloak d'un utilisateur (bloque la connexion).
     * Appelé quand tous les diplômes d'un médecin sont rejetés.
     */
    public void deactivateUser(String userId) {
        try {
            restClient.patch()
                    .uri("/internal/users/{userId}/deactivate", userId)
                    .header("X-Internal-Api-Key", internalApiKey)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Compte désactivé dans Identity pour userId={}", userId);
        } catch (RestClientException e) {
            log.error("Impossible de désactiver le compte Identity pour userId={} : {}", userId, e.getMessage());
        }
    }
}
