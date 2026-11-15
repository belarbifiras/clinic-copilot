package com.clinic.ai_assessment_service.triage;

import com.clinic.ai_assessment_service.config.AiProperties;
import com.clinic.ai_assessment_service.directory.DirectoryClient;
import com.clinic.ai_assessment_service.directory.DoctorSuggestion;
import com.clinic.ai_assessment_service.domain.FallbackReason;
import com.clinic.ai_assessment_service.domain.SessionStatus;
import com.clinic.ai_assessment_service.domain.TriageAssessment;
import com.clinic.ai_assessment_service.domain.TriageSource;
import com.clinic.ai_assessment_service.domain.UrgencyLevel;
import com.clinic.ai_assessment_service.events.TriageEvent;
import com.clinic.ai_assessment_service.events.TriageEventPublisher;
import com.clinic.ai_assessment_service.llm.ModelOutcome;
import com.clinic.ai_assessment_service.llm.ModelRunner;
import com.clinic.ai_assessment_service.llm.TriageContext;
import com.clinic.ai_assessment_service.persistence.TriageSession;
import com.clinic.ai_assessment_service.persistence.TriageSessionRepository;
import com.clinic.ai_assessment_service.rules.EmergencyMatch;
import com.clinic.ai_assessment_service.rules.EmergencyRuleEngine;
import com.clinic.ai_assessment_service.util.Json;
import com.clinic.ai_assessment_service.util.Pseudonymizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Pipeline de triage :
 * 1) règles d'urgence déterministes (N1 forcé, sans LLM) ;
 * 2) LLM local (Ollama) avec sortie JSON validée + principe de précaution ;
 * 3) repli de sécurité (médecine générale, N3, alerte administrateur) si le LLM
 * échoue ;
 * 4) persistance + événement.
 * Ce service ne réserve AUCUN rendez-vous : c'est le rôle de
 * appointment-service.
 * Pas de @Transactional volontairement : l'appel LLM (long) ne doit pas garder
 * une connexion BD ouverte.
 */
@Service
public class TriageService {

    private static final Logger log = LoggerFactory.getLogger(TriageService.class);

    private final AiProperties props;
    private final EmergencyRuleEngine rules;
    private final ModelRunner runner;
    private final TriageSessionRepository repo;
    private final TriageEventPublisher events;
    private final DirectoryClient directory;

    public TriageService(AiProperties props, EmergencyRuleEngine rules, ModelRunner runner,
            TriageSessionRepository repo, TriageEventPublisher events, DirectoryClient directory) {
        this.props = props;
        this.rules = rules;
        this.runner = runner;
        this.repo = repo;
        this.events = events;
        this.directory = directory;
    }

    public TriageSessionResponse start(String patientRef, StartTriageRequest req) {
        checkLocation(req.latitude(), req.longitude());
        TriageSession s = new TriageSession();
        s.id = UUID.randomUUID();
        s.patientRef = patientRef;
        s.language = req.language();
        s.symptoms = Pseudonymizer.scrub(req.symptoms()).strip();
        s.age = req.age();
        s.sex = req.sex();
        s.allergiesJson = Json.write(req.allergies() == null ? List.of() : req.allergies());
        s.historyJson = Json.write(req.medicalHistory() == null ? List.of() : req.medicalHistory());
        s.durationDays = req.durationDays();
        s.intensity = req.intensity();
        s.promptVersion = props.promptVersion();
        s.modelName = props.llm().model();

        EmergencyMatch match = rules.evaluate(s.symptoms);
        if (match.triggered()) {
            completeWithEmergency(s, match);
        } else {
            runModel(s, !props.clarificationEnabled());
        }
        TriageSession saved = repo.save(s);
        publishEvents(saved);
        return toResponse(saved, req.latitude(), req.longitude());
    }

    public TriageSessionResponse answer(UUID id, String patientRef, AnswerRequest req) {
        checkLocation(req.latitude(), req.longitude());
        TriageSession s = load(id, patientRef);
        if (s.status != SessionStatus.NEEDS_CLARIFICATION) {
            throw new ConflictException("Cette session n'attend pas de réponses.");
        }
        List<String> questions = Json.readStringList(s.clarificationQuestionsJson);
        if (req.answers().size() != questions.size()) {
            throw new IllegalArgumentException("Le nombre de réponses doit être égal au nombre de questions.");
        }
        List<String> answers = req.answers().stream().map(a -> Pseudonymizer.scrub(a).strip()).toList();
        s.clarificationAnswersJson = Json.write(answers);

        // Les réponses peuvent révéler un signe d'urgence : les règles repassent
        // d'abord.
        EmergencyMatch match = rules.evaluate(
                Stream.concat(Stream.of(s.symptoms), answers.stream()).toArray(String[]::new));
        if (match.triggered()) {
            completeWithEmergency(s, match);
        } else {
            runModel(s, true);
        }
        repo.save(s);
        publishEvents(s);
        return toResponse(s, req.latitude(), req.longitude());
    }

    public TriageSessionResponse get(UUID id, String patientRef) {
        return toResponse(load(id, patientRef), null, null);
    }

    // ---------------------------------------------------------------- internals

    private TriageSession load(UUID id, String patientRef) {
        // 404 aussi si la session appartient à un autre patient : on ne révèle pas son
        // existence.
        return repo.findByIdAndPatientRef(id, patientRef)
                .orElseThrow(() -> new NotFoundException("Session introuvable."));
    }

    private void runModel(TriageSession s, boolean finalRound) {
        TriageContext ctx = new TriageContext(
                s.language, s.age, s.sex,
                Json.readStringList(s.allergiesJson), Json.readStringList(s.historyJson),
                s.symptoms, s.durationDays, s.intensity,
                Json.readStringList(s.clarificationQuestionsJson),
                Json.readStringList(s.clarificationAnswersJson));

        ModelOutcome outcome = runner.run(ctx, finalRound);

        if (outcome.assessment() == null) {
            completeWithFallback(s, outcome);
            return;
        }
        TriageAssessment a = outcome.assessment();
        s.modelName = outcome.modelName();
        s.modelDigest = outcome.modelDigest();
        s.precautionAdjustments = String.join(",", outcome.adjustments());

        boolean askQuestions = !finalRound
                && a.urgencyLevel() != UrgencyLevel.N1
                && !a.followupQuestions().isEmpty();
        if (askQuestions) {
            s.status = SessionStatus.NEEDS_CLARIFICATION;
            s.clarificationQuestionsJson = Json.write(a.followupQuestions());
            // Explication provisoire conservée pour l'affichage (sans niveau d'urgence ni réservation tant que status != COMPLETED).
            s.resultJson = Json.write(a);
            return;
        }
        complete(s, a.withoutFollowups(), TriageSource.LLM);
    }

    private void completeWithEmergency(TriageSession s, EmergencyMatch match) {
        complete(s, Messages.emergency(s.language, match.ruleIds()), TriageSource.RULES);
        s.modelName = null;
        s.modelDigest = null;
        log.info("triage_emergency_rule session={} rules={}", s.id, match.ruleIds());
    }

    private void completeWithFallback(TriageSession s, ModelOutcome outcome) {
        FallbackReason reason = outcome.fallbackReason();
        s.fallbackReason = reason;
        s.adminAlerted = true;
        complete(s, Messages.fallback(s.language, reason.name()), TriageSource.FALLBACK);
        log.warn("ADMIN_ALERT triage fallback used: session={} reason={}", s.id, reason);
    }

    private void complete(TriageSession s, TriageAssessment a, TriageSource source) {
        s.status = SessionStatus.COMPLETED;
        s.source = source;
        s.urgencyLevel = a.urgencyLevel();
        s.confidence = a.confidence();
        s.recommendedSpecialty = a.recommendedSpecialty();
        s.resultJson = Json.write(a);
    }

    private void publishEvents(TriageSession s) {
        if (s.status != SessionStatus.COMPLETED) {
            return;
        }
        events.publish(new TriageEvent("COMPLETED", s.id, s.urgencyLevel, s.source));
        if (s.urgencyLevel == UrgencyLevel.N1 || s.urgencyLevel == UrgencyLevel.N2) {
            events.publish(new TriageEvent("URGENT", s.id, s.urgencyLevel, s.source));
        }
        if (s.adminAlerted) {
            events.publish(new TriageEvent("ADMIN_ALERT", s.id, s.urgencyLevel, s.source));
        }
    }

    private static void checkLocation(Double lat, Double lng) {
        if ((lat == null) != (lng == null)) {
            throw new IllegalArgumentException("latitude et longitude doivent être fournies ensemble.");
        }
    }

    private TriageSessionResponse toResponse(TriageSession s, Double lat, Double lng) {
        boolean completed = s.status == SessionStatus.COMPLETED;
        TriageAssessment stored = s.resultJson == null ? null : Json.read(s.resultJson, TriageAssessment.class);
        TriageAssessment a = completed ? stored : null;
        String preliminary = !completed && stored != null ? stored.patientExplanation() : null;
        boolean emergency = a != null && a.urgencyLevel() == UrgencyLevel.N1;
        boolean booking = a != null && !emergency;
        List<String> questions = s.status == SessionStatus.NEEDS_CLARIFICATION
                ? Json.readStringList(s.clarificationQuestionsJson)
                : List.of();
        // N1 : consignes d'attente fixes (générales + une par règle déclenchée). Jamais écrites par le LLM.
        List<String> interim = emergency
                ? Messages.interimAdvice(s.language,
                        s.source == TriageSource.RULES ? a.redFlagsDetected() : List.of())
                : List.of();
        // Médecins proches : jamais en N1, seulement si le patient a fourni sa position. Non conservée.
        List<DoctorSuggestion> doctors = booking && lat != null && lng != null
                ? directory.nearest(a.recommendedSpecialty(), lat, lng)
                : List.of();
        return new TriageSessionResponse(
                s.id, s.status.name(), s.language, questions, preliminary, a,
                s.source == null ? null : s.source.name(),
                emergency, booking, interim, doctors, s.modelName, s.promptVersion,
                Messages.disclaimer(s.language), s.createdAt);
    }
}