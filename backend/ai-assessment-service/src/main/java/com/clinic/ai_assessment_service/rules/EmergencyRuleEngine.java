package com.clinic.ai_assessment_service.rules;

import com.clinic.ai_assessment_service.util.TextNormalizer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Règles d'urgence DÉTERMINISTES, exécutées AVANT tout appel au LLM.
 * Si une règle se déclenche, le niveau N1 est forcé sans attendre le modèle.
 *
 * Limites connues (voulues, côté précaution) :
 * - pas de gestion de la négation ("pas de douleur thoracique" déclenche aussi la règle) ;
 * - la liste de mots-clés est un BROUILLON : elle doit être validée par un médecin.
 */
@Component
public class EmergencyRuleEngine {

    private static final Logger log = LoggerFactory.getLogger(EmergencyRuleEngine.class);
    private static final String RULES_RESOURCE = "triage/emergency-rules.json";

    private record Rule(String id, List<String> patterns, List<String> wholeWords) {
    }

    private final List<Rule> rules = new ArrayList<>();
    private final String version;

    public EmergencyRuleEngine() {
        try (InputStream in = new ClassPathResource(RULES_RESOURCE).getInputStream()) {
            JsonNode root = new ObjectMapper().readTree(in);
            this.version = root.path("version").asText("unknown");
            for (JsonNode r : root.path("rules")) {
                List<String> patterns = new ArrayList<>();
                for (JsonNode p : r.path("patterns")) {
                    patterns.add(TextNormalizer.normalize(p.asText()));
                }
                List<String> words = new ArrayList<>();
                for (JsonNode w : r.path("wholeWords")) {
                    words.add(TextNormalizer.normalize(w.asText()));
                }
                rules.add(new Rule(r.path("id").asText(), patterns, words));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de charger " + RULES_RESOURCE, e);
        }
        log.info("Emergency rules loaded: version={} count={}", version, rules.size());
    }

    public String version() {
        return version;
    }

    /** Évalue un ou plusieurs textes (symptômes, réponses de clarification). */
    public EmergencyMatch evaluate(String... texts) {
        String padded = TextNormalizer.padded(String.join(" . ", texts));
        List<String> matched = new ArrayList<>();
        for (Rule rule : rules) {
            boolean hit = false;
            for (String p : rule.patterns()) {
                if (!p.isEmpty() && padded.contains(p)) {
                    hit = true;
                    break;
                }
            }
            if (!hit) {
                for (String w : rule.wholeWords()) {
                    if (!w.isEmpty() && padded.contains(" " + w + " ")) {
                        hit = true;
                        break;
                    }
                }
            }
            if (hit) {
                matched.add(rule.id());
            }
        }
        return new EmergencyMatch(!matched.isEmpty(), List.copyOf(matched));
    }
}
