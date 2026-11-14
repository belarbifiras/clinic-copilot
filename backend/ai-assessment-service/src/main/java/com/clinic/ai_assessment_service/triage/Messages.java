package com.clinic.ai_assessment_service.triage;

import com.clinic.ai_assessment_service.domain.PossibleCondition;
import com.clinic.ai_assessment_service.domain.TriageAssessment;
import com.clinic.ai_assessment_service.domain.UrgencyLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Textes fixes (avertissement, urgence, repli) en fr / ar / en.
 * Aucun numéro d'urgence n'est écrit ici : il doit venir d'une configuration validée localement.
 * Ces formulations sont à relire par l'équipe / un professionnel de santé.
 */
final class Messages {

    private Messages() {
    }

    private static final Map<String, String> DISCLAIMER = Map.of(
            "fr", "Cette orientation est fournie par une intelligence artificielle à titre indicatif uniquement. "
                    + "Elle ne constitue pas un diagnostic et ne remplace pas l'avis d'un médecin.",
            "en", "This orientation is provided by an artificial intelligence for guidance only. "
                    + "It is not a diagnosis and does not replace a physician's advice.",
            "ar", "هذا التوجيه صادر عن نظام ذكاء اصطناعي لأغراض إرشادية فقط، ولا يُعدّ تشخيصًا طبيًا ولا يغني عن استشارة الطبيب.");

    private static final Map<String, String> EMERGENCY = Map.of(
            "fr", "Vos symptômes peuvent correspondre à une urgence vitale. Appelez immédiatement les secours "
                    + "ou rendez-vous aux urgences les plus proches, sans attendre un rendez-vous.",
            "en", "Your symptoms may indicate a life-threatening emergency. Call emergency services immediately "
                    + "or go to the nearest emergency department, without waiting for an appointment.",
            "ar", "قد تدل أعراضك على حالة طارئة تهدد الحياة. اتصل بخدمات الطوارئ فورًا أو توجّه إلى أقرب قسم مستعجلات "
                    + "دون انتظار موعد.");

    private static final Map<String, String> FALLBACK = Map.of(
            "fr", "Nous n'avons pas pu analyser vos symptômes automatiquement. Nous vous conseillons de consulter "
                    + "un médecin généraliste dans les 72 heures. En cas d'aggravation ou de symptômes graves, "
                    + "appelez immédiatement les secours.",
            "en", "We could not analyze your symptoms automatically. We advise you to see a general practitioner "
                    + "within 72 hours. If your condition worsens or you have severe symptoms, call emergency services immediately.",
            "ar", "تعذّر تحليل أعراضك آليًا. ننصحك بزيارة طبيب عام خلال 72 ساعة. وإذا ساءت حالتك أو ظهرت أعراض شديدة "
                    + "فاتصل بخدمات الطوارئ فورًا.");

    static String disclaimer(String lang) {
        return DISCLAIMER.getOrDefault(lang, DISCLAIMER.get("en"));
    }

    static TriageAssessment emergency(String lang, List<String> ruleIds) {
        return new TriageAssessment(
                UrgencyLevel.N1, 1.0, List.<PossibleCondition>of(), "emergency",
                ruleIds, EMERGENCY.getOrDefault(lang, EMERGENCY.get("en")),
                "Emergency rule(s) triggered: " + String.join(", ", ruleIds) + ". No LLM was used. Immediate emergency care advised.",
                List.of());
    }

    static TriageAssessment fallback(String lang, String reason) {
        return new TriageAssessment(
                UrgencyLevel.N3, 0.0, List.<PossibleCondition>of(), "general_medicine",
                List.of(), FALLBACK.getOrDefault(lang, FALLBACK.get("en")),
                "FALLBACK (" + reason + "): automatic analysis unavailable. General medicine, semi-urgent (N3) by precaution. Manual review recommended.",
                List.of());
    }

    // ------------------------------------------------------------------------------------------
    // Consignes d'attente en N1 : textes FIXES, jamais générés par le LLM, sans médicament ni dosage.
    // BROUILLON : à relire et valider par un médecin (et l'arabe par un locuteur natif) avant toute démonstration.
    // Elles passent APRÈS l'appel des secours et ne les remplacent jamais.
    // ------------------------------------------------------------------------------------------
    private static final Map<String, List<String>> ADVICE_GENERIC = Map.of(
            "fr", List.of(
                    "Appelez les secours maintenant, ou demandez à une personne proche de le faire.",
                    "Ne restez pas seul(e) : prévenez quelqu'un près de vous.",
                    "Ne conduisez pas vous-même."),
            "en", List.of(
                    "Call emergency services now, or ask someone nearby to do it.",
                    "Do not stay alone: tell someone close to you.",
                    "Do not drive yourself."),
            "ar", List.of(
                    "اتصل بخدمات الطوارئ الآن، أو اطلب من شخص قريب أن يتصل.",
                    "لا تبقَ وحدك، وأخبر شخصًا قريبًا منك.",
                    "لا تقُد السيارة بنفسك."));

    private static final Map<String, Map<String, List<String>>> ADVICE_BY_RULE = Map.of(
            "CHEST_PAIN", Map.of(
                    "fr", List.of("Arrêtez toute activité et asseyez-vous, ou mettez-vous en position semi-assise.",
                            "Desserrez les vêtements qui serrent le cou ou la poitrine."),
                    "en", List.of("Stop all activity and sit down, or rest in a half-sitting position.",
                            "Loosen clothing around your neck and chest."),
                    "ar", List.of("توقّف عن أي نشاط واجلس، أو خذ وضعية نصف جلوس.",
                            "أرخِ الملابس الضيقة حول العنق والصدر.")),
            "BREATHING_DIFFICULTY", Map.of(
                    "fr", List.of("Asseyez-vous bien droit(e) et penchez-vous légèrement en avant.",
                            "Ouvrez une fenêtre, desserrez vos vêtements et essayez de respirer lentement et calmement."),
                    "en", List.of("Sit upright and lean slightly forward.",
                            "Open a window, loosen your clothing and try to breathe slowly and calmly."),
                    "ar", List.of("اجلس بشكل مستقيم وانحنِ قليلًا إلى الأمام.",
                            "افتح نافذة، وأرخِ ملابسك، وحاول أن تتنفس ببطء وهدوء.")),
            "LOSS_OF_CONSCIOUSNESS", Map.of(
                    "fr", List.of("Si la personne respire, allongez-la sur le côté (position latérale de sécurité).",
                            "Si elle ne respire pas ou ne réagit pas, suivez les instructions des secours au téléphone."),
                    "en", List.of("If the person is breathing, lay them on their side (recovery position).",
                            "If they are not breathing or not responding, follow the emergency operator's instructions on the phone."),
                    "ar", List.of("إذا كان الشخص يتنفس، ضعه مستلقيًا على جانبه (وضعية الأمان الجانبية).",
                            "إذا لم يكن يتنفس أو لا يستجيب، اتبع تعليمات خدمات الطوارئ على الهاتف.")),
            "HEAVY_BLEEDING", Map.of(
                    "fr", List.of("Appuyez fermement sur la plaie avec un linge propre et maintenez la pression.",
                            "Si le linge se tache, ajoutez-en un par-dessus sans retirer le premier. Allongez la personne si possible."),
                    "en", List.of("Press firmly on the wound with a clean cloth and keep the pressure on.",
                            "If the cloth soaks through, add another on top without removing the first. Lay the person down if possible."),
                    "ar", List.of("اضغط بقوة على الجرح بقطعة قماش نظيفة واستمر في الضغط.",
                            "إذا تبلل القماش، ضع قطعة أخرى فوقه دون إزالة الأولى، واجعل المصاب يستلقي إن أمكن.")),
            "STROKE_SIGNS", Map.of(
                    "fr", List.of("Notez l'heure exacte à laquelle les signes sont apparus.",
                            "Ne donnez ni nourriture ni boisson. Gardez la personne installée confortablement, la tête légèrement surélevée."),
                    "en", List.of("Note the exact time the signs started.",
                            "Do not give food or drink. Keep the person comfortable with the head slightly raised."),
                    "ar", List.of("دوّن الوقت الدقيق الذي بدأت فيه الأعراض.",
                            "لا تُعطِ الشخص طعامًا ولا شرابًا، وأبقِه في وضعية مريحة مع رفع رأسه قليلًا.")),
            "SUICIDAL_THOUGHTS", Map.of(
                    "fr", List.of("Vous n'êtes pas seul(e) : appelez les secours ou une personne de confiance maintenant, et restez avec quelqu'un.",
                            "Éloignez-vous de tout objet ou produit qui pourrait vous blesser."),
                    "en", List.of("You are not alone: call emergency services or someone you trust now, and stay with someone.",
                            "Move away from anything that could hurt you."),
                    "ar", List.of("لستَ وحدك: اتصل بخدمات الطوارئ أو بشخص تثق به الآن، وابقَ مع شخص ما.",
                            "ابتعد عن أي شيء قد يؤذيك.")));

    /** ruleIds : identifiants des règles déclenchées (vide si N1 vient du modèle : seules les consignes générales sont données). */
    static List<String> interimAdvice(String lang, List<String> ruleIds) {
        String l = ADVICE_GENERIC.containsKey(lang) ? lang : "en";
        List<String> out = new ArrayList<>(ADVICE_GENERIC.get(l));
        for (String id : ruleIds) {
            Map<String, List<String>> byLang = ADVICE_BY_RULE.get(id);
            if (byLang != null) {
                out.addAll(byLang.get(l));
            }
        }
        return List.copyOf(out);
    }
}