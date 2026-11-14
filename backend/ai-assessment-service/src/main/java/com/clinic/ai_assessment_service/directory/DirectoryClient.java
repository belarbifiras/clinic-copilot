package com.clinic.ai_assessment_service.directory;

import com.clinic.ai_assessment_service.util.Json;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Appel serveur à serveur vers doctor-directory-service (annuaire public, médecins vérifiés et actifs).
 * - Le LLM n'intervient pas : la distance est calculée par l'annuaire.
 * - La position du patient n'est ni journalisée ni conservée.
 * - Toute erreur (annuaire arrêté, réponse inattendue) donne une liste vide : le triage n'est jamais bloqué.
 */
@Component
public class DirectoryClient {

    private static final Logger log = LoggerFactory.getLogger(DirectoryClient.class);
    private static final int MAX_RESULTS = 3;

    private final RestClient client;

    public DirectoryClient(@Value("${directory.base-url:}") String baseUrl,
                           @Value("${directory.timeout-seconds:3}") int timeoutSeconds) {
        RestClient c = null;
        if (baseUrl == null || baseUrl.isBlank()) {
            log.warn("directory.base-url non configuré : recherche du médecin le plus proche désactivée");
        } else {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(Duration.ofSeconds(timeoutSeconds));
            factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
            c = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
        }
        this.client = c;
    }

    /** specialtyCode : code de l'annuaire (l'annuaire le met lui-même en majuscules, ex. DERMATOLOGY). */
    public List<DoctorSuggestion> nearest(String specialtyCode, double lat, double lng) {
        if (client == null || specialtyCode == null || specialtyCode.isBlank()) {
            return List.of();
        }
        try {
            String body = client.get()
                    .uri(u -> u.path("/api/directory/doctors")
                            .queryParam("specialty", specialtyCode.toUpperCase(Locale.ROOT))
                            .queryParam("lat", lat)
                            .queryParam("lng", lng)
                            .queryParam("sort", "distance")
                            .queryParam("size", MAX_RESULTS)
                            .build())
                    .retrieve()
                    .body(String.class);
            if (body == null) {
                return List.of();
            }
            JsonNode root = Json.MAPPER.readTree(body);
            JsonNode items = root.path("content");
            if (!items.isArray()) {
                items = root.path("items");
            }
            List<DoctorSuggestion> out = new ArrayList<>();
            for (JsonNode d : items) {
                String name = String.join(" ",
                        d.path("title").asText(""), d.path("firstName").asText(""), d.path("lastName").asText(""))
                        .trim().replaceAll("\\s+", " ");
                JsonNode dist = d.path("distanceKm");
                out.add(new DoctorSuggestion(
                        d.path("id").asText(),
                        name,
                        d.path("city").asText(null),
                        d.path("governorate").asText(null),
                        dist.isNumber() ? dist.asDouble() : null));
                if (out.size() == MAX_RESULTS) {
                    break;
                }
            }
            return List.copyOf(out);
        } catch (Exception e) {
            // Jamais de position, ni de réponse brute dans les logs.
            log.warn("directory_lookup_failed type={}", e.getClass().getSimpleName());
            return List.of();
        }
    }
}
