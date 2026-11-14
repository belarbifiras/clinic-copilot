package com.clinic.ai_assessment_service.web;

import com.clinic.ai_assessment_service.triage.AnswerRequest;
import com.clinic.ai_assessment_service.triage.StartTriageRequest;
import com.clinic.ai_assessment_service.triage.TriageService;
import com.clinic.ai_assessment_service.triage.TriageSessionResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * IMPORTANT (temporaire) : l'identité du patient arrive dans l'en-tête X-Patient-Id.
 * C'est falsifiable si la gateway ne l'impose pas. À remplacer par le "subject" du JWT
 * dès que la configuration JWT (gateway / identity-profile-service) sera vérifiée.
 * La gateway ne doit jamais transmettre tel quel un X-Patient-Id fourni par le client.
 */
@RestController
@RequestMapping("/triage/sessions")
@Validated
public class TriageController {

    private static final String PATIENT_HEADER = "X-Patient-Id";

    private final TriageService service;

    public TriageController(TriageService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TriageSessionResponse start(
            @RequestHeader(PATIENT_HEADER) @Size(min = 1, max = 64) @Pattern(regexp = "[A-Za-z0-9._-]+") String patientRef,
            @Valid @RequestBody StartTriageRequest request) {
        return service.start(patientRef, request);
    }

    @PostMapping("/{id}/answers")
    public TriageSessionResponse answer(
            @PathVariable UUID id,
            @RequestHeader(PATIENT_HEADER) @Size(min = 1, max = 64) @Pattern(regexp = "[A-Za-z0-9._-]+") String patientRef,
            @Valid @RequestBody AnswerRequest request) {
        return service.answer(id, patientRef, request);
    }

    @GetMapping("/{id}")
    public TriageSessionResponse get(
            @PathVariable UUID id,
            @RequestHeader(PATIENT_HEADER) @Size(min = 1, max = 64) @Pattern(regexp = "[A-Za-z0-9._-]+") String patientRef) {
        return service.get(id, patientRef);
    }
}
