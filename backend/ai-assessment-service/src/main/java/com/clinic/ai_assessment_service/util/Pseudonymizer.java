package com.clinic.ai_assessment_service.util;

import java.util.regex.Pattern;

/**
 * Retire les identifiants directs évidents (emails, numéros de téléphone) du texte libre.
 * LIMITE : les noms propres ne sont pas détectés de façon fiable. Le prompt demande aussi
 * au patient de ne pas saisir d'identité, mais cela ne remplace pas une vraie anonymisation.
 */
public final class Pseudonymizer {

    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+(?:\\.[\\w-]+)+");
    private static final Pattern PHONE = Pattern.compile("(?<![\\w])\\+?\\d[\\d\\s().-]{6,}\\d(?![\\w])");

    private Pseudonymizer() {
    }

    public static String scrub(String text) {
        if (text == null) {
            return "";
        }
        String s = EMAIL.matcher(text).replaceAll("[EMAIL]");
        return PHONE.matcher(s).replaceAll("[PHONE]");
    }
}
