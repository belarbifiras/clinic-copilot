package com.clinic.copilot.ui.booking

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clinic.copilot.domain.Appointment
import com.clinic.copilot.navigation.AppBottomNavigationBar
import com.clinic.copilot.navigation.OngletNavigation
import com.clinic.copilot.ui.theme.AppointmentColors
import com.clinic.copilot.ui.theme.AppointmentFormatters
import com.clinic.copilot.ui.theme.AppointmentTheme
import com.clinic.copilot.ui.theme.ChatTypography

/**
 * Écran de confirmation d'une réservation ou d'une reprogrammation (US-31 / US-34).
 *
 * @param onRetour Retour manuel vers la sélection de créneau.
 * @param onConflit Retour automatique après un 409 et rechargement des créneaux.
 * @param onTermine Retour vers la liste des rendez-vous après succès.
 * @param onRdvClick Action de l'onglet « RDV » de la navigation basse.
 * @param viewModel ViewModel injecté par Hilt.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingConfirmationScreen(
    onRetour: () -> Unit,
    onConflit: () -> Unit,
    onTermine: () -> Unit,
    onRdvClick: () -> Unit = {},
    viewModel: BookingConfirmationViewModel = hiltViewModel(),
) {
    AppointmentTheme {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val contexte = LocalContext.current
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(state.conflit) {
            if (state.conflit) {
                snackbarHostState.showSnackbar(
                    state.errorMessage ?: "Ce créneau vient d'être réservé par un autre patient",
                )
                onConflit()
            }
        }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
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
                        Text(
                            text = if (state.estReprogrammation) {
                                "Confirmer la reprogrammation"
                            } else {
                                "Confirmer le rendez-vous"
                            },
                            style = ChatTypography.headlineSm,
                            color = MaterialTheme.colorScheme.primary,
                        )
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
            val rendezVous = state.rendezVous
            if (rendezVous != null) {
                ConfirmationReussie(
                    rendezVous = rendezVous,
                    surAjouterCalendrier = {
                        val ajoute = CalendarIntentHelper.ouvrirCalendrier(
                            contexte = contexte,
                            titre = "Rendez-vous - ${rendezVous.doctorName}",
                            lieu = rendezVous.lieu,
                            debut = rendezVous.heureDebut,
                            fin = rendezVous.heureFin,
                        )
                        if (!ajoute) {
                            Toast.makeText(
                                contexte,
                                "Aucune application calendrier n'est disponible.",
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    },
                    surTerminer = onTermine,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding)
                        .padding(16.dp),
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    CarteRecapitulative(
                        doctorName = state.doctorName,
                        specialite = state.specialite,
                        date = state.date,
                        heureDebut = state.heureDebut,
                        heureFin = state.heureFin,
                    )

                    state.errorMessage?.let { message ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = AppointmentColors.errorContainer40,
                            shadowElevation = 0.dp,
                        ) {
                            Text(
                                text = message,
                                style = ChatTypography.bodyMd,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(12.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = viewModel::confirmer,
                        enabled = !state.enCours,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary,
                            disabledContainerColor = AppointmentColors.surfaceContainer,
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    ) {
                        if (state.enCours) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onSecondary,
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.width(10.dp))
                        } else {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                        }
                        Text(
                            text = if (state.estReprogrammation) {
                                "Confirmer la reprogrammation"
                            } else {
                                "Confirmer le rendez-vous"
                            },
                            style = ChatTypography.labelLg,
                        )
                    }
                }
            }
        }
    }
}

/** Carte récapitulant le médecin et le créneau choisi. */
@Composable
private fun CarteRecapitulative(
    doctorName: String,
    specialite: String,
    date: Long,
    heureDebut: Long,
    heureFin: Long,
) {
    val forme = RoundedCornerShape(12.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = forme,
                ambientColor = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.2f),
                spotColor = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.2f),
            ),
        shape = forme,
        color = AppointmentColors.surfaceContainerLowest,
        shadowElevation = 0.dp,
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = AppointmentFormatters.initiale(doctorName),
                        style = ChatTypography.titleMd,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        doctorName,
                        style = ChatTypography.headlineSm,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        specialite,
                        style = ChatTypography.bodySm,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = AppointmentFormatters.formaterDateComplete(date),
                style = ChatTypography.titleMd,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = AppointmentFormatters.formaterHeure(heureDebut, heureFin),
                    style = ChatTypography.labelLg,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

/** Écran de succès avec validation verte et actions calendrier / fin. */
@Composable
private fun ConfirmationReussie(
    rendezVous: Appointment,
    surAjouterCalendrier: () -> Unit,
    surTerminer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(32.dp))
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Rendez-vous confirmé",
            modifier = Modifier.size(72.dp),
            tint = AppointmentColors.emerald500,
        )
        Text(
            text = "Rendez-vous confirmé",
            style = ChatTypography.headlineMd,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = AppointmentFormatters.formaterDateHeure(rendezVous),
            style = ChatTypography.bodyMd,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = surAjouterCalendrier,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppointmentColors.surfaceContainerLow,
                contentColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            Text("Ajouter au calendrier", style = ChatTypography.labelLg)
        }
        Button(
            onClick = surTerminer,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
            ),
        ) {
            Text("Terminer", style = ChatTypography.labelLg)
        }
    }
}
