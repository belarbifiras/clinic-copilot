package com.clinic.ai_assessment_service.llm;

import com.clinic.ai_assessment_service.config.AiProperties;
import com.clinic.ai_assessment_service.domain.PossibleCondition;
import com.clinic.ai_assessment_service.domain.TriageAssessment;
import com.clinic.ai_assessment_service.domain.UrgencyLevel;
import com.clinic.ai_assessment_service.util.Json;
import com.clinic.ai_assessment_service.util.TextNormalizer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validation STRICTE de la sortie du LLM côté backend (le schéma envoyé à Ollama ne suffit pas).
 * Rejette les sorties invalides, toute posologie et les médicaments non demandés explicitement.
 * En cas de rejet, l'appelant applique le repli : médecine générale, N3, alerte administrateur.
 */
@Component
public class TriageOutputValidator {

    private static final Set<String> ALLOWED_FIELDS = Set.of(
            "urgency_level", "confidence", "possible_conditions", "recommended_specialty",
            "red_flags_detected", "patient_explanation", "doctor_summary", "followup_questions",
            "consultation_recommended");

    private static final Pattern DOSAGE =
            Pattern.compile("(?i)(?<![\\p{L}\\d])\\d+(?:[.,]\\d+)?\\s?(?:mg|mcg|ml|g|ui|iu)(?![\\p{L}\\d])");

    /** Liste minimale d'exemples ; à compléter avec un pharmacien / médecin. */
    private static final List<String> DRUG_NAMES = List.of(
            "paracetamol", "doliprane", "ibuprofen", "ibuprofene", "aspirin", "aspirine",
            "amoxicillin", "amoxicilline", "diclofenac", "tramadol", "codeine", "morphine",
            "باراسيتامول", "ايبوبروفين", "اسبرين", "اموكسيسيلين");

    private final Set<String> allowedSpecialties;

    @Autowired
    public TriageOutputValidator(AiProperties props) {
        this(props.allowedSpecialties());
    }

    public TriageOutputValidator(List<String> allowedSpecialties) {
        this.allowedSpecialties = Set.copyOf(allowedSpecialties);
    }

    public TriageAssessment parseAndValidate(String json) throws InvalidModelOutputException {
        return parseAndValidate(json, false);
    }

    TriageAssessment parseAndValidate(String json, boolean medicationQuestion) throws InvalidModelOutputException {
        JsonNode root;
        try {
            root = Json.MAPPER.readTree(json);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            throw new InvalidModelOutputException("not_json");
        }
        if (root == null || !root.isObject()) {
            throw new InvalidModelOutputException("not_an_object");
        }
        Iterator<String> names = root.fieldNames();
        while (names.hasNext()) {
            if (!ALLOWED_FIELDS.contains(names.next())) {
                throw new InvalidModelOutputException("unknown_field");
            }
        }

        UrgencyLevel level;
        try {
            level = UrgencyLevel.valueOf(text(root, "urgency_level", 2, 2));
        } catch (IllegalArgumentException e) {
            throw new InvalidModelOutputException("bad_urgency_level");
        }
        double confidence = number(root, "confidence");

        JsonNode condNode = array(root, "possible_conditions", 5);
        List<PossibleCondition> conditions = new ArrayList<>();
        for (JsonNode c : condNode) {
            if (!c.isObject()) {
                throw new InvalidModelOutputException("bad_condition");
            }
            conditions.add(new PossibleCondition(text(c, "label", 1, 120), number(c, "likelihood")));
        }

        String specialty = text(root, "recommended_specialty", 1, 80);
        if (!allowedSpecialties.contains(specialty)) {
            throw new InvalidModelOutputException("unknown_specialty");
        }

        List<String> redFlags = stringList(root, "red_flags_detected", 10, 200);
        String patientExplanation = text(root, "patient_explanation", 1, 1500);
        String doctorSummary = text(root, "doctor_summary", 1, 1500);
        List<String> followups = stringList(root, "followup_questions", 5, 300);
        boolean consultationRecommended = bool(root, "consultation_recommended");

        StringBuilder all = new StringBuilder(patientExplanation).append(' ').append(doctorSummary);
        conditions.forEach(c -> all.append(' ').append(c.label()));
        followups.forEach(f -> all.append(' ').append(f));
        if (containsDosage(all.toString()) || (!medicationQuestion && containsDrugName(all.toString()))) {
            throw new InvalidModelOutputException("medication_or_dosage_mentioned");
        }

        return new TriageAssessment(level, confidence, List.copyOf(conditions), specialty,
                List.copyOf(redFlags), patientExplanation, doctorSummary, List.copyOf(followups),
                consultationRecommended);
    }

    static boolean containsMedication(String text) {
        return containsDosage(text) || containsDrugName(text);
    }

    static boolean containsDosage(String text) {
        return DOSAGE.matcher(text).find();
    }

    static boolean containsDrugName(String text) {
        String normalized = TextNormalizer.normalize(text);
        for (String drug : DRUG_NAMES) {
            if (normalized.contains(TextNormalizer.normalize(drug))) {
                return true;
            }
        }
        return false;
    }

    private static boolean bool(JsonNode node, String field) throws InvalidModelOutputException {
        JsonNode value = node.get(field);
        if (value == null || !value.isBoolean()) {
            throw new InvalidModelOutputException("missing_or_not_boolean:" + field);
        }
        return value.booleanValue();
    }

    private static String text(JsonNode node, String field, int min, int max) throws InvalidModelOutputException {
        JsonNode v = node.get(field);
        if (v == null || !v.isTextual()) {
            throw new InvalidModelOutputException("missing_or_not_text:" + field);
        }
        String s = v.asText().strip();
        if (s.length() < min || s.length() > max) {
            throw new InvalidModelOutputException("bad_length:" + field);
        }
        return s;
    }

    private static double number(JsonNode node, String field) throws InvalidModelOutputException {
        JsonNode v = node.get(field);
        if (v == null || !v.isNumber()) {
            throw new InvalidModelOutputException("missing_or_not_number:" + field);
        }
        double d = v.asDouble();
        if (Double.isNaN(d) || d < 0.0 || d > 1.0) {
            throw new InvalidModelOutputException("out_of_range:" + field);
        }
        return d;
    }

    private static JsonNode array(JsonNode node, String field, int maxItems) throws InvalidModelOutputException {
        JsonNode v = node.get(field);
        if (v == null || !v.isArray() || v.size() > maxItems) {
            throw new InvalidModelOutputException("bad_array:" + field);
        }
        return v;
    }

    private static List<String> stringList(JsonNode node, String field, int maxItems, int maxLen)
            throws InvalidModelOutputException {
        List<String> out = new ArrayList<>();
        for (JsonNode item : array(node, field, maxItems)) {
            if (!item.isTextual() || item.asText().isBlank() || item.asText().length() > maxLen) {
                throw new InvalidModelOutputException("bad_item:" + field);
            }
            out.add(item.asText().strip());
        }
        return out;
    }
}
