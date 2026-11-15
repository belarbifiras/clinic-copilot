package com.clinic.ai_assessment_service.llm;

import com.clinic.ai_assessment_service.config.AiProperties;
import com.clinic.ai_assessment_service.util.Json;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Client HTTP vers le serveur d'inférence (FastAPI + modèle Hugging Face sur Colab).
 * Route appelée : POST /generate. Aucun contenu patient dans les erreurs.
 */
@Component
public class LlmClient {

    private final AiProperties props;
    private final RestClient rest;

    public LlmClient(AiProperties props) {
        this.props = props;
        AiProperties.Llm l = props.llm();
        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(l.connectTimeoutSeconds()))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(l.readTimeoutSeconds()));
        this.rest = RestClient.builder()
                .baseUrl(l.baseUrl() == null ? "" : l.baseUrl())
                .requestFactory(factory)
                .defaultHeader("X-API-Key", l.apiKey() == null ? "" : l.apiKey())
                .defaultHeader("ngrok-skip-browser-warning", "1")
                .build();
    }

    public OllamaReply chat(String systemPrompt, String userPrompt, ObjectNode schema)
            throws OllamaUnavailableException {
        ObjectNode body = Json.MAPPER.createObjectNode();
        body.put("system", systemPrompt);
        body.put("user", userPrompt);
        body.set("json_schema", schema);
        try {
            byte[] payload = Json.write(body).getBytes(StandardCharsets.UTF_8);
            byte[] raw = rest.post()
                    .uri("/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(byte[].class);
            if (raw == null) {
                throw new OllamaUnavailableException("empty_response", null);
            }
            JsonNode root = Json.MAPPER.readTree(new String(raw, StandardCharsets.UTF_8));
            String content = root.path("content").asText("");
            String model = root.path("model").asText(props.llm().model());
            return new OllamaReply(content, model);
        } catch (OllamaUnavailableException e) {
            throw e;
        } catch (Exception e) {
            throw new OllamaUnavailableException("llm_call_failed: " + e.getClass().getSimpleName(), e);
        }
    }

    /** Empreinte du modèle : non fournie pour l'instant (traçabilité à améliorer). */
    public String modelDigest() {
        return "unknown";
    }
}