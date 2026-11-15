package com.clinic.ai_assessment_service.rules;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Cas SYNTHÉTIQUES uniquement. Objectif du cahier des charges : 100 % des cas vitaux en N1. */
class EmergencyRuleEngineTest {

    private final EmergencyRuleEngine engine = new EmergencyRuleEngine();

    private static final List<String> LIFE_THREATENING = List.of(
            // français
            "J'ai une douleur thoracique depuis 20 minutes",
            "J'ai une oppression thoracique forte",
            "Je n'arrive pas à respirer",
            "Mon père a perdu connaissance",
            "Elle saigne beaucoup et ne s'arrête pas",
            "Je pense avoir un AVC, mon visage qui tombe",
            "J'ai des idées suicidaires",
            // anglais
            "I have chest pain and it spreads to my arm",
            "I can't breathe properly",
            "He passed out and is not responding",
            "There is heavy bleeding from the wound",
            "I think it is a stroke, slurred speech",
            "I want to kill myself",
            // arabe
            "عندي ألم في الصدر منذ ساعة",
            "عندي صعوبة في التنفس",
            "فقدان الوعي مفاجئ",
            "نزيف حاد من الجرح",
            "اريد ان اموت");

    private static final List<String> NOT_EMERGENCY = List.of(
            "J'ai mal à la gorge depuis trois jours avec un peu de fièvre",
            "I have a mild headache and a runny nose",
            "عندي سعال خفيف وزكام منذ يومين",
            "Une éruption cutanée légère sur le bras");

    @Test
    void allLifeThreateningCasesTriggerN1() {
        for (String text : LIFE_THREATENING) {
            assertTrue(engine.evaluate(text).triggered(), "Doit déclencher une règle d'urgence : " + text);
        }
    }

    @Test
    void benignCasesDoNotTrigger() {
        for (String text : NOT_EMERGENCY) {
            assertFalse(engine.evaluate(text).triggered(), "Ne doit pas déclencher : " + text);
        }
    }

    @Test
    void wholeWordRuleMatchesAvcButNotInsideOtherWords() {
        assertTrue(engine.evaluate("Suspicion d'AVC").triggered());
        assertFalse(engine.evaluate("avcxyz").triggered());
    }

    @Test
    void emergencyInClarificationAnswersIsDetected() {
        assertTrue(engine.evaluate("mal de tête", "oui, j'ai aussi une douleur thoracique").triggered());
    }
}
