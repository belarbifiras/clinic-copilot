package com.clinic.ai_assessment_service.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Normalisation de texte multilingue (français, anglais, arabe) pour les règles d'urgence :
 * minuscules, suppression des accents et des diacritiques arabes, unification de quelques lettres arabes,
 * ponctuation remplacée par des espaces.
 */
public final class TextNormalizer {

    private static final Pattern MARKS = Pattern.compile("[\\p{Mn}\\u0640]");
    private static final Pattern NON_ALNUM = Pattern.compile("[^\\p{L}\\p{N}]+");

    private TextNormalizer() {
    }

    public static String normalize(String input) {
        if (input == null) {
            return "";
        }
        String s = Normalizer.normalize(input, Normalizer.Form.NFD);
        s = MARKS.matcher(s).replaceAll("");
        s = s.toLowerCase(Locale.ROOT);
        s = s.replace('ى', 'ي').replace('ة', 'ه');
        s = NON_ALNUM.matcher(s).replaceAll(" ").trim();
        return s;
    }

    /** Texte normalisé entouré d'espaces, pour la recherche de mots entiers. */
    public static String padded(String input) {
        return " " + normalize(input) + " ";
    }
}
