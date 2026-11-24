package com.clinic.copilot.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clinic.copilot.ui.theme.ChatColors
import com.clinic.copilot.ui.theme.ChatTypography

/** Onglets de la barre de navigation basse partagée entre messagerie et rendez-vous. */
enum class OngletNavigation {

    /** Accueil (TODO : navigation par rôle, US-05). */
    ACCUEIL,

    /** Annuaire des médecins (TODO : navigation par rôle, US-05). */
    MEDECINS,

    /** Rendez-vous patient, branché sur le graphe local des rendez-vous. */
    RDV,

    /** Messagerie, onglet par défaut de la feature messagerie. */
    MESSAGES,

    /** Profil (TODO : navigation par rôle, US-05). */
    PROFIL,
}

/**
 * Barre d'onglets locale de niveau 4, partagée par la messagerie et la feature rendez-vous.
 *
 * Le composable est identique dans les deux features : seul l'onglet actif et sa couleur
 * changent. Sur les écrans rendez-vous, l'onglet « RDV » est actif en `secondary` avec un
 * texte en gras ; les onglets Accueil / Médecins / Profil conservent leur TODO d'origine.
 *
 * @param ongletActif Onglet couramment affiché.
 * @param onRdvClick Action de l'onglet « RDV ».
 * @param couleurActive Couleur de l'onglet sélectionné (secondary côté rendez-vous).
 * @param modifier Modificateur appliqué à la surface.
 * @param couleurInactive Couleur des onglets non sélectionnés.
 */
@Composable
fun AppBottomNavigationBar(
    ongletActif: OngletNavigation,
    onRdvClick: () -> Unit,
    couleurActive: Color,
    modifier: Modifier = Modifier,
    couleurInactive: Color = ChatColors.textSecondary,
) {
    val forme = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    val elements = listOf(
        ElementNavigation("Accueil", OngletNavigation.ACCUEIL, Icons.Default.Home) {
            // TODO: navigation par rôle (US-05)
        },
        ElementNavigation("Médecins", OngletNavigation.MEDECINS, Icons.Default.People) {
            // TODO: navigation par rôle (US-05)
        },
        ElementNavigation("RDV", OngletNavigation.RDV, Icons.Default.CalendarMonth, onRdvClick),
        ElementNavigation("Messages", OngletNavigation.MESSAGES, Icons.Default.ChatBubble) {
            // TODO: navigation par rôle (US-05)
        },
        ElementNavigation("Profil", OngletNavigation.PROFIL, Icons.Default.Person) {
            // TODO: navigation par rôle (US-05)
        },
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = forme,
                ambientColor = ChatColors.primaryNavy.copy(alpha = 0.12f),
                spotColor = ChatColors.primaryNavy.copy(alpha = 0.12f),
            ),
        shape = forme,
        // Fallback : translucidité 94 % ; le backdrop-blur 16dp n'est pas disponible nativement.
        color = Color(0xF0FFFFFF),
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            elements.forEach { element ->
                val actif = element.onglet == ongletActif
                val couleur = if (actif) couleurActive else couleurInactive
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable(onClick = element.action),
                ) {
                    Icon(
                        imageVector = element.icone,
                        contentDescription = element.libelle,
                        tint = couleur,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.height(3.dp))
                    if (actif) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(couleur),
                        )
                    } else {
                        Spacer(Modifier.size(6.dp))
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = element.libelle,
                        style = ChatTypography.bodySm.copy(
                            color = couleur,
                            fontWeight = if (actif) FontWeight.SemiBold else FontWeight.Normal,
                        ),
                    )
                }
            }
        }
    }
}

/**
 * Entrée d'un onglet de la navigation basse.
 *
 * @property libelle Libellé affiché sous l'icône.
 * @property onglet Onglet représenté.
 * @property icone Icône Material de l'onglet.
 * @property action Callback exécuté au clic ; les onglets non branchés gardent un TODO.
 */
private data class ElementNavigation(
    val libelle: String,
    val onglet: OngletNavigation,
    val icone: ImageVector,
    val action: () -> Unit,
)
