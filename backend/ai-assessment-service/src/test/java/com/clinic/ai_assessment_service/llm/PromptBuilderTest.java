package com.clinic.ai_assessment_service.llm;

import com.clinic.ai_assessment_service.config.AiProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PromptBuilderTest {

    private final PromptBuilder builder = new PromptBuilder(new AiProperties(
            "test", "fr", 0.5, true, 1, 1,
            new AiProperties.Llm("", "", "qwen", 1, 1),
            new AiProperties.Ollama("", "qwen", 1, 1, 1, 1, 0.1, "1m"),
            List.of("general_medicine", "dermatology")));

    @Test
    void requiresTunisianDerjaForTunisianArabicInputs() {
        String prompt = builder.systemPrompt("ar", false);

        assertTrue(prompt.contains("Tunisian Derja"));
        assertTrue(prompt.contains("use Arabic script"));
        assertTrue(prompt.contains("b9a3 k7al"));
        assertTrue(prompt.contains("do not switch to French"));
        assertTrue(prompt.contains("doctor_summary' MUST be written in French"));
        assertTrue(prompt.contains("consultation_recommended=false"));
        assertTrue(prompt.contains("Never prescribe a medicine"));
    }

    @Test
    void requiresResponsesInTheDetectedLanguage() {
        assertTrue(builder.systemPrompt("fr", true).contains("written in French"));
        assertTrue(builder.systemPrompt("en", true).contains("written in English"));
        assertTrue(builder.systemPrompt("ar", true).contains("written in Arabic"));
    }
}
