package com.clinic.ai_assessment_service.llm;

import com.clinic.ai_assessment_service.domain.UrgencyLevel;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;

/** Schéma JSON envoyé à Ollama (paramètre "format") pour contraindre la sortie du modèle. */
public final class TriageJsonSchema {

    private TriageJsonSchema() {
    }

    public static ObjectNode build(ObjectMapper m, List<String> specialties) {
        ObjectNode schema = m.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = schema.putObject("properties");

        ArrayNode levels = props.putObject("urgency_level").put("type", "string").putArray("enum");
        for (UrgencyLevel l : UrgencyLevel.values()) {
            levels.add(l.name());
        }
        props.putObject("confidence").put("type", "number");

        ObjectNode cond = props.putObject("possible_conditions").put("type", "array").putObject("items");
        cond.put("type", "object");
        ObjectNode condProps = cond.putObject("properties");
        condProps.putObject("label").put("type", "string");
        condProps.putObject("likelihood").put("type", "number");
        cond.putArray("required").add("label").add("likelihood");
        cond.put("additionalProperties", false);

        ArrayNode spec = props.putObject("recommended_specialty").put("type", "string").putArray("enum");
        specialties.forEach(spec::add);

        props.putObject("red_flags_detected").put("type", "array").putObject("items").put("type", "string");
        props.putObject("patient_explanation").put("type", "string");
        props.putObject("doctor_summary").put("type", "string");
        props.putObject("followup_questions").put("type", "array").putObject("items").put("type", "string");

        ArrayNode required = schema.putArray("required");
        List.of("urgency_level", "confidence", "possible_conditions", "recommended_specialty",
                "red_flags_detected", "patient_explanation", "doctor_summary", "followup_questions")
                .forEach(required::add);
        schema.put("additionalProperties", false);
        return schema;
    }
}
