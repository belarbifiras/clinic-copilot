package com.clinic.copilot.ui.slots

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clinic.copilot.domain.TimeSlot
import com.clinic.copilot.navigation.AppBottomNavigationBar
import com.clinic.copilot.navigation.OngletNavigation
import com.clinic.copilot.ui.theme.AppointmentColors
import com.clinic.copilot.ui.theme.AppointmentFormatters
import com.clinic.copilot.ui.theme.AppointmentTheme
import com.clinic.copilot.ui.theme.ChatTypography

/**
 * Paramètres transmis à l'écran de confirmation après le choix d'un créneau.
 *
 * @property doctorId Identifiant du médecin.
 * @property doctorName Nom affiché du médecin.
 * @property specialite Spécialité du médecin.
 * @property slotId Identifiant du créneau choisi.
 * @property date Jour du créneau.
 * @property heureDebut Début du créneau.
 * @property heureFin Fin du créneau.
 * @property appointmentId Identifiant du rendez-vous à reprogrammer, ou vide pour une réservation.
 */
data class ParametresConfirmation(
    val doctorId: String,
    val doctorName: String,
    val specialite: String,
    val slotId: String,
    val date: Long,
    val heureDebut: Long,
    val heureFin: Long,
    val appointmentId: String = "",
)

/**
 * Écran de sélection d'un jour puis d'un créneau, conservé pour la reprogrammation (US-34).
 *
 * La nouvelle réservation est intégrée à l'écran « Mes rendez-vous » : ce parcours n'est
 * atteint que depuis les actions « Reprogrammer ».
 *
 * @param onRetour Retour vers l'écran précédent.
 * @param onContinuer Callback appelé avec le créneau choisi.
 * @param onRdvClick Action de l'onglet « RDV » de la navigation basse.
 * @param viewModel ViewModel injecté par Hilt.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlotSelectionScreen(
    onRetour: () -> Unit,
    onContinuer: (ParametresConfirmation) -> Unit,
    onRdvClick: () -> Unit = {},
    viewModel: SlotSelectionViewModel = hiltViewModel(),
) {
    AppointmentTheme {
        val state by viewModel.uiState.collectAsStateWithLifecycle()

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                AppBottomNavigationBar(
                    ongletActif = OngletNavigation.RDV,
                    onRdvClick = onRdvClick,
                    couleurActive = MaterialTheme.colorScheme.secondary,
                    couleurInactive = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = if (state.estReprogrammation) {
                                    "Reprogrammer le rendez-vous"
                                } else {
                                    "Choisir un créneau"
                                },
                                style = ChatTypography.headlineSm,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = "${state.doctorName} · ${state.specialite}",
                                style = ChatTypography.bodySm,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onRetour) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Retour",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = AppointmentColors.surfaceContainerLowest,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                        navigationIconContentColor = MaterialTheme.colorScheme.primary,
                    ),
                )
            },
        ) { contentPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EditCalendar,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = "Sélectionnez un jour puis un horaire",
                        style = ChatTypography.labelMd,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.dates, key = { it }) { date ->
                        ChipDate(
                            date = date,
                            selectionne = date == state.dateSelectionnee,
                            onClick = { viewModel.selectionnerDate(date) },
                        )
                    }
                }

                when (state.etat) {
                    EtatSlots.CHARGEMENT -> {
                        Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                        }
                    }

                    EtatSlots.ERREUR -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = state.errorMessage ?: "Impossible de charger les créneaux.",
                                style = ChatTypography.bodyMd,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                            )
                            Button(
                                onClick = viewModel::actualiser,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                    contentColor = MaterialTheme.colorScheme.onSecondary,
                                ),
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(Modifier.size(8.dp))
                                Text("Réessayer", style = ChatTypography.labelLg)
                            }
                        }
                    }

                    EtatSlots.VIDE -> {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = AppointmentColors.surfaceContainerLow,
                            shadowElevation = 0.dp,
                        ) {
                            Text(
                                text = "Aucun créneau disponible pour ce jour.",
                                style = ChatTypography.bodyMd,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(20.dp),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                    EtatSlots.DONNEES -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(state.creneaux, key = { it.id }) { creneau ->
                                ChipCreneau(
                                    creneau = creneau,
                                    selectionne = creneau.id == state.creneauSelectionne?.id,
                                    onClick = { viewModel.selectionnerCreneau(creneau) },
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = {
                        state.creneauSelectionne?.let { creneau ->
                            onContinuer(
                                ParametresConfirmation(
                                    doctorId = state.doctorId,
                                    doctorName = state.doctorName,
                                    specialite = state.specialite,
                                    slotId = creneau.id,
                                    date = state.dateSelectionnee,
                                    heureDebut = creneau.heureDebut,
                                    heureFin = creneau.heureFin,
                                    appointmentId = state.rendezVousId,
                                ),
                            )
                        }
                    },
                    enabled = state.peutContinuer,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                        disabledContainerColor = AppointmentColors.surfaceContainer,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = if (state.estReprogrammation) {
                            "Confirmer le nouveau créneau"
                        } else {
                            "Continuer"
                        },
                        style = ChatTypography.labelLg,
                    )
                }
            }
        }
    }
}

/** Chip de jour sélectionné en secondary, sinon surface claire. */
@Composable
private fun ChipDate(date: Long, selectionne: Boolean, onClick: () -> Unit) {
    val (jour, numero) = AppointmentFormatters.formaterJourChip(date)
    val forme = RoundedCornerShape(50)
    Surface(
        modifier = Modifier
            .height(48.dp)
            .clip(forme)
            .clickable(onClick = onClick),
        shape = forme,
        color = if (selectionne) {
            MaterialTheme.colorScheme.secondary
        } else {
            AppointmentColors.surfaceContainerLow
        },
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = if (selectionne) {
                    MaterialTheme.colorScheme.onSecondary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = "$jour $numero",
                style = ChatTypography.labelMd,
                color = if (selectionne) {
                    MaterialTheme.colorScheme.onSecondary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }
    }
}

/** Chip pill d'un créneau, désactivé visuellement quand le créneau est pris. */
@Composable
private fun ChipCreneau(creneau: TimeSlot, selectionne: Boolean, onClick: () -> Unit) {
    val forme = RoundedCornerShape(50)
    val heure = AppointmentFormatters.formaterHeure(creneau.heureDebut, creneau.heureFin)
        .substringBefore(" - ")
    Surface(
        modifier = Modifier
            .height(36.dp)
            .fillMaxWidth()
            .alpha(if (creneau.disponible) 1f else 0.4f)
            .clip(forme)
            .then(
                if (selectionne) {
                    Modifier.shadow(
                        elevation = 4.dp,
                        shape = forme,
                        ambientColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
                        spotColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
                    )
                } else {
                    Modifier
                },
            )
            .clickable(enabled = creneau.disponible, onClick = onClick),
        shape = forme,
        color = if (selectionne) {
            MaterialTheme.colorScheme.secondary
        } else {
            AppointmentColors.surfaceContainerLow
        },
        shadowElevation = 0.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = heure,
                style = ChatTypography.labelMd,
                color = if (selectionne) {
                    MaterialTheme.colorScheme.onSecondary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }
    }
}
