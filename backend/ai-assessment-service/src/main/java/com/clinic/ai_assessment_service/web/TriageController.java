package com.clinic.ai_assessment_service.web;

import com.clinic.ai_assessment_service.triage.AnswerRequest;
import com.clinic.ai_assessment_service.triage.StartTriageRequest;
import com.clinic.ai_assessment_service.triage.TriageService;
import com.clinic.ai_assessment_service.triage.TriageSessionResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** L'identité du patient est le "sub" du jeton Keycloak (non falsifiable). */
@RestController
@RequestMapping("/triage/sessions")
@Validated
public class TriageController {

    private final TriageService service;

    public TriageController(TriageService service) {
        this.service = service;
    }

    private static String patientRef(Authentication auth) {
        if (auth instanceof JwtAuthenticationToken t) {
            return t.getToken().getSubject();
        }
        return auth.getName();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TriageSessionResponse start(Authentication auth, @Valid @RequestBody StartTriageRequest request) {
        return service.start(patientRef(auth), request);
    }

    @PostMapping("/{id}/answers")
    public TriageSessionResponse answer(@PathVariable UUID id, Authentication auth,
                                        @Valid @RequestBody AnswerRequest request) {
        return service.answer(id, patientRef(auth), request);
    }

    @GetMapping("/{id}")
    public TriageSessionResponse get(@PathVariable UUID id, Authentication auth) {
        return service.get(id, patientRef(auth));
    }
}