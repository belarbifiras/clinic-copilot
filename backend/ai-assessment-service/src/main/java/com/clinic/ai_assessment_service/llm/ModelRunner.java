package com.clinic.ai_assessment_service.llm;

import com.clinic.ai_assessment_service.config.AiProperties;
import com.clinic.ai_assessment_service.domain.FallbackReason;
import com.clinic.ai_assessment_service.domain.TriageAssessment;
import com.clinic.ai_assessment_service.util.Json;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Orchestre un appel au LLM : file d'attente bornée, appel Ollama, validation stricte, principe de précaution.
 * Ne lève jamais d'exception métier : en cas de problème, renvoie un repli (voir ModelOutcome.fallback).
 * Le LLM n'a AUCUN accès à la base de données ni aux réservations.
 */
@Component
public class ModelRunner {

    private static final Logger log = LoggerFactory.getLogger(ModelRunner.class);

    private final AiProperties props;
    private final LlmClient ollama;
    private final PromptBuilder prompts;
    private final TriageOutputValidator validator;
    private final PrecautionPolicy policy;
    private final Semaphore gate;
    private final ObjectNode schema;

    public ModelRunner(AiProperties props, LlmClient ollama, PromptBuilder prompts,
                       TriageOutputValidator validator, PrecautionPolicy policy) {
        this.props = props;
        this.ollama = ollama;
        this.prompts = prompts;
        this.validator = validator;
        this.policy = policy;
        this.gate = new Semaphore(Math.max(1, props.maxConcurrentRequests()), true);
        this.schema = TriageJsonSchema.build(Json.MAPPER, props.allowedSpecialties());
    }

    public ModelOutcome run(TriageContext ctx, boolean finalRound) {
        String configuredModel = props.llm().model();
        long start = System.nanoTime();
        boolean acquired = false;
        String result = "ok";
        try {
            acquired = gate.tryAcquire(props.queueWaitSeconds(), TimeUnit.SECONDS);
            if (!acquired) {
                result = "busy";
                return ModelOutcome.fallback(FallbackReason.BUSY, configuredModel);
            }
            OllamaReply reply = ollama.chat(
                    prompts.systemPrompt(ctx.language(), finalRound), prompts.userPrompt(ctx), schema);
            String patientText = ctx.symptoms() + " " + String.join(" ", ctx.answers());
            boolean medicationQuestion = TriageOutputValidator.containsDrugName(patientText);
            TriageAssessment parsed = validator.parseAndValidate(reply.content(), medicationQuestion);
            PrecautionPolicy.PolicyResult pr = policy.apply(parsed);
            return ModelOutcome.ok(pr.assessment(), reply.model(), ollama.modelDigest(), pr.adjustments());
        } catch (OllamaUnavailableException e) {
            result = "ollama_unavailable";
            log.warn("LLM unavailable: {}", e.getMessage());
            return ModelOutcome.fallback(FallbackReason.OLLAMA_UNAVAILABLE, configuredModel);
        } catch (InvalidModelOutputException e) {
            result = "invalid_output";
            log.warn("LLM output rejected: reason={}", e.getMessage());
            return ModelOutcome.fallback(FallbackReason.INVALID_OUTPUT, configuredModel);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            result = "interrupted";
            return ModelOutcome.fallback(FallbackReason.OLLAMA_UNAVAILABLE, configuredModel);
        } finally {
            if (acquired) {
                gate.release();
            }
            // Jamais de texte patient dans les logs : seulement durée et résultat.
            log.info("triage_llm result={} durationMs={}", result, (System.nanoTime() - start) / 1_000_000);
        }
    }
}
