package com.clinic.ai_assessment_service.llm;

import com.clinic.ai_assessment_service.config.AiProperties;
import com.clinic.ai_assessment_service.util.Json;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Client HTTP vers Ollama (API /api/chat). Ollama doit rester sur le réseau privé.
 * Les corps de requête/réponse sont échangés en octets UTF-8 pour préserver l'arabe.
 */
@Component
public class OllamaClient {

    private static final Logger log = LoggerFactory.getLogger(OllamaClient.class);

    private final AiProperties props;
    private final RestClient rest;
    private final AtomicReference<String> cachedDigest = new AtomicReference<>();

    public OllamaClient(AiProperties props) {
        this.props = props;
        AiProperties.Ollama o = props.ollama();
        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(o.connectTimeoutSeconds()))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(o.readTimeoutSeconds()));
        this.rest = RestClient.builder().baseUrl(o.baseUrl()).requestFactory(factory).build();
    }

    public OllamaReply chat(String systemPrompt, String userPrompt, ObjectNode schema)
            throws OllamaUnavailableException {
        AiProperties.Ollama o = props.ollama();
        ObjectNode body = Json.MAPPER.createObjectNode();
        body.put("model", o.model());
        body.put("stream", false);
        body.set("format", schema);
        body.put("keep_alive", o.keepAlive());
        ArrayNode messages = body.putArray("messages");
        messages.addObject().put("role", "system").put("content", systemPrompt);
        messages.addObject().put("role", "user").put("content", userPrompt);
        ObjectNode options = body.putObject("options");
        options.put("temperature", o.temperature());
        options.put("num_ctx", o.numCtx());
        options.put("num_predict", o.numPredict());

        try {
            byte[] payload = Json.write(body).getBytes(StandardCharsets.UTF_8);
            byte[] raw = rest.post()
                    .uri("/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(byte[].class);
            if (raw == null) {
                throw new OllamaUnavailableException("empty_response", null);
            }
            JsonNode root = Json.MAPPER.readTree(new String(raw, StandardCharsets.UTF_8));
            String content = root.path("message").path("content").asText("");
            String model = root.path("model").asText(o.model());
            // Temps fournis par Ollama (nanosecondes -> ms). Aucun contenu patient dans ce log.
            log.info("ollama_timing loadMs={} promptTokens={} promptMs={} genTokens={} genMs={}",
                    root.path("load_duration").asLong(0) / 1_000_000,
                    root.path("prompt_eval_count").asLong(0),
                    root.path("prompt_eval_duration").asLong(0) / 1_000_000,
                    root.path("eval_count").asLong(0),
                    root.path("eval_duration").asLong(0) / 1_000_000);
            return new OllamaReply(content, model);
        } catch (OllamaUnavailableException e) {
            throw e;
        } catch (Exception e) {
            // Message volontairement court : jamais de contenu patient dans les erreurs.
            throw new OllamaUnavailableException("ollama_call_failed: " + e.getClass().getSimpleName(), e);
        }
    }

    /** Empreinte (digest) du modèle configuré, pour la traçabilité. "unknown" si indisponible. */
    public String modelDigest() {
        String cached = cachedDigest.get();
        if (cached != null) {
            return cached;
        }
        try {
            byte[] raw = rest.get().uri("/api/tags").retrieve().body(byte[].class);
            if (raw != null) {
                JsonNode models = Json.MAPPER.readTree(new String(raw, StandardCharsets.UTF_8)).path("models");
                String wanted = props.ollama().model();
                for (JsonNode m : models) {
                    String name = m.path("name").asText("");
                    if (name.equals(wanted) || name.startsWith(wanted + ":") || wanted.startsWith(name)){
                        String digest = m.path("digest").asText("unknown");
                        cachedDigest.set(digest);
                        return digest;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Model digest unavailable: {}", e.getClass().getSimpleName());
        }
        return "unknown";
    }
}
