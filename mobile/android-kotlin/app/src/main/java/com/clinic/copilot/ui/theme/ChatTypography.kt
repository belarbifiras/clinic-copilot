package com.clinic.copilot.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

// TODO: remplacer par Plus Jakarta Sans une fois les fichiers de police fournis par l'équipe design system (US-02)

/**
 * Échelle typographique officielle de la feature messagerie (Plus Jakarta Sans).
 *
 * Les tailles, poids, interlignes et espacements de lettres sont repris à l'identique
 * du design system ; seule la famille de police reste provisoire.
 */
object ChatTypography {

    /** Titres d'écran secondaires : 18sp / 600 / 24sp. */
    val headlineSm = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    )

    /** Titres de cartes et de barre d'outils : 16sp / 600 / 22sp. */
    val titleMd = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    )

    /** Corps de texte principal : 14sp / 400 / 20sp. */
    val bodyMd = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    )

    /** Corps de texte secondaire : 12sp / 400 / 16sp. */
    val bodySm = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    )

    /** Libellés mis en avant : 14sp / 600 / 20sp / 0.01em. */
    val labelLg = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.01.em,
    )

    /** Petits libellés et badges : 11sp / 700 / 14sp / 0.04em. */
    val labelSm = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.04.em,
    )
}
