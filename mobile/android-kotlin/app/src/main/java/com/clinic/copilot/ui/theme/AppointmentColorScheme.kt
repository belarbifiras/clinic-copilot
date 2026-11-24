package com.clinic.copilot.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// TODO: ce ColorScheme correspond aux tokens officiels du design system — à terme, le thème global de l'application (US-02) devrait l'adopter entièrement

/**
 * ColorScheme Material3 exacte de la feature Rendez-vous, issue de la référence Stitch.
 *
 * Toutes les valeurs ci-dessous sont les tokens officiels : aucune approximation.
 * Ce schéma est appliqué uniquement par [AppointmentTheme], jamais au thème global.
 */
val AppointmentColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF001844),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF102D63),
    onPrimaryContainer = Color(0xFF7E96D2),
    secondary = Color(0xFF0055C6),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF216EED),
    onSecondaryContainer = Color(0xFFFEFCFF),
    tertiary = Color(0xFF001F21),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF003639),
    onTertiaryContainer = Color(0xFF00A8AF),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    background = Color(0xFFF8F9FF),
    onBackground = Color(0xFF0D1C2E),
    surface = Color(0xFFF8F9FF),
    onSurface = Color(0xFF0D1C2E),
    surfaceVariant = Color(0xFFD4E4FC),
    onSurfaceVariant = Color(0xFF444650),
    outline = Color(0xFF747781),
    outlineVariant = Color(0xFFC4C6D1),
    inverseSurface = Color(0xFF223144),
    inverseOnSurface = Color(0xFFEAF1FF),
    inversePrimary = Color(0xFFB0C6FF),
    surfaceTint = Color(0xFF455D95),
)

/**
 * Containers Material3 étendus et couleurs utilitaires de badges.
 *
 * Ces valeurs ne font pas partie du `lightColorScheme` standard : elles sont exposées
 * séparément et référencées directement par les composables de la feature.
 */
object AppointmentColors {

    /** Surface la plus haute : cartes blanches. */
    val surfaceContainerLowest: Color = Color(0xFFFFFFFF)

    /** Surface basse : blocs internes aux cartes. */
    val surfaceContainerLow: Color = Color(0xFFEFF4FF)

    /** Surface intermédiaire. */
    val surfaceContainer: Color = Color(0xFFE5EEFF)

    /** Surface haute : badges et encarts « Copilot ». */
    val surfaceContainerHigh: Color = Color(0xFFDCE9FF)

    /** Teinte claire des badges « Confirmé ». */
    val emerald50: Color = Color(0xFFECFDF5)

    /** Point des badges « Confirmé ». */
    val emerald500: Color = Color(0xFF10B981)

    /** Texte des badges « Confirmé ». */
    val emerald700: Color = Color(0xFF047857)

    /** Teinte claire des badges « En attente ». */
    val amber50: Color = Color(0xFFFFFBEB)

    /** Point des badges « En attente ». */
    val amber500: Color = Color(0xFFF59E0B)

    /** Texte des badges « En attente ». */
    val amber800: Color = Color(0xFF92400E)

    /** Version translucide de [AppointmentColorScheme.errorContainer] pour la bannière d'urgence. */
    val errorContainer40: Color = AppointmentColorScheme.errorContainer.copy(alpha = 0.4f)
}

/**
 * Thème local de la feature Rendez-vous.
 *
 * À utiliser UNIQUEMENT autour des écrans de cette feature : le thème global partagé
 * n'est pas modifié.
 *
 * @param content Contenu Compose utilisant ce ColorScheme.
 */
@Composable
fun AppointmentTheme(content: @Composable () -> Unit) {
    // Le design ne prévoit qu'un schéma clair : aucune variante sombre n'est appliquée.
    MaterialTheme(colorScheme = AppointmentColorScheme, content = content)
}
