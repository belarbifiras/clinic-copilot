package com.clinic.copilot.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Tokens de couleur officiels du design system, strictement limités à la feature messagerie.
 *
 * Aucune couleur approximative n'est conservée : toutes les valeurs ci-dessous
 * proviennent du design system officiel transmis pour cette feature.
 */
object ChatColors {

    /** Fondations structurelles, headers, texte haute importance. */
    val primaryNavy: Color = Color(0xFF102D63)

    /** Boutons d'action, états actifs, navigation sélectionnée. */
    val primaryBlue: Color = Color(0xFF1769E8)

    /** Exclusivement pour l'IA Copilot, badges de triage, glow. */
    val turquoiseAccent: Color = Color(0xFF13B9C1)

    /** Fond des prompts IA, cartes sélectionnées. */
    val lightBlueSurface: Color = Color(0xFFEAF5FF)

    /** Fond de tous les écrans. */
    val medicalBackground: Color = Color(0xFFF7FAFE)

    /** Surface des cartes. */
    val cardSurface: Color = Color(0xFFFFFFFF)

    /** Texte principal. */
    val textMain: Color = Color(0xFF14213D)

    /** Texte secondaire. */
    val textSecondary: Color = Color(0xFF718096)

    /** Niveau de triage critique. */
    val urgenceCritique: Color = Color(0xFFE53E3E)

    /** Niveau de triage consultation urgente. */
    val consultationUrgente: Color = Color(0xFFED8936)

    /** Niveau de triage routine médicale. */
    val routineMedicale: Color = Color(0xFF1769E8)

    /** Niveau de triage accompagnement IA. */
    val accompagnementIA: Color = Color(0xFF13B9C1)

    /** État de validation réussie. */
    val validationSucces: Color = Color(0xFF38A169)

    /** Teinte des badges confirmés. */
    val badgeTintConfirme: Color = Color(0xFFEAF5FF)

    /** Teinte des badges IA (12 % d'opacité). */
    val badgeTintIA: Color = Color(0x1F13B9C1)
}
