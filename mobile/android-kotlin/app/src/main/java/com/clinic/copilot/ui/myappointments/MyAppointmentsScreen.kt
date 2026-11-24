package com.clinic.copilot.ui.myappointments

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Brightness3
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.NotificationImportant
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clinic.copilot.R
import com.clinic.copilot.domain.Appointment
import com.clinic.copilot.domain.SpecialiteRendezVous
import com.clinic.copilot.domain.StatutRendezVous
import com.clinic.copilot.domain.TimeSlot
import com.clinic.copilot.navigation.AppBottomNavigationBar
import com.clinic.copilot.navigation.OngletNavigation
import com.clinic.copilot.ui.theme.AppointmentColors
import com.clinic.copilot.ui.theme.AppointmentFormatters
import com.clinic.copilot.ui.theme.AppointmentTheme
import com.clinic.copilot.ui.theme.ChatTypography

/**
 * Écran « Mes rendez-vous » (US-32 à US-35) reproduisant la référence Stitch.
 *
 * Tout est présent sur un seul écran scrollable : bannière d'urgence, segmented control,
 * carte featured, cartes compactes et carte de réservation intégrée. La logique reste dans
 * [MyAppointmentsViewModel] ; le composable n'affiche que l'état et transmet les intentions.
 *
 * @param onReprogrammer Callback ouvert avec le rendez-vous à déplacer (US-34).
 * @param onRdvClick Action de l'onglet « RDV » de la navigation basse.
 * @param viewModel ViewModel injecté par Hilt.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyAppointmentsScreen(
    onReprogrammer: (Appointment) -> Unit,
    onRdvClick: () -> Unit = {},
    viewModel: MyAppointmentsViewModel = hiltViewModel(),
) {
    AppointmentTheme {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val contexte = LocalContext.current
        val snackbarHostState = remember { SnackbarHostState() }
        var menuOuvert by remember { mutableStateOf(false) }

        // Rafraîchissement silencieux à chaque retour au premier plan (après réservation).
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner, viewModel) {
            val observateur = LifecycleEventObserver { _, evenement ->
                if (evenement == Lifecycle.Event.ON_RESUME) {
                    viewModel.charger(afficherChargement = false)
                }
            }
            lifecycleOwner.lifecycle.addObserver(observateur)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observateur) }
        }

        val messageSnackbar = state.messageSnackbar
        LaunchedEffect(messageSnackbar) {
            if (messageSnackbar != null) {
                snackbarHostState.showSnackbar(messageSnackbar, duration = SnackbarDuration.Long)
                viewModel.consommerMessageSnackbar()
            }
        }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ImageMascotte()
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Clinic Copilot",
                                    style = ChatTypography.headlineSm,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text = "Rdv",
                                    style = ChatTypography.labelSm,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    actions = {
                        Box {
                            IconButton(onClick = { menuOuvert = true }) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notifications et options",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (state.rappelsActifs) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 8.dp, end = 8.dp)
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.error),
                                )
                            }
                        }
                        // Le menu US-32 est conservé dans la cloche du nouveau design.
                        DropdownMenu(
                            expanded = menuOuvert,
                            onDismissRequest = { menuOuvert = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Actualiser", style = ChatTypography.bodyMd) },
                                onClick = {
                                    menuOuvert = false
                                    viewModel.charger()
                                },
                            )
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("Trier par date", style = ChatTypography.bodyMd)
                                        Text(
                                            text = "Actuel : ${state.ordreTri.libelle}",
                                            style = ChatTypography.bodySm,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                },
                                onClick = {
                                    menuOuvert = false
                                    viewModel.basculerTri()
                                },
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = if (state.rappelsActifs) {
                                            "Désactiver les rappels"
                                        } else {
                                            "Activer les rappels"
                                        },
                                        style = ChatTypography.bodyMd,
                                    )
                                },
                                onClick = {
                                    menuOuvert = false
                                    viewModel.basculerRappels()
                                },
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = AppointmentColors.surfaceContainerLowest,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            },
            bottomBar = {
                AppBottomNavigationBar(
                    ongletActif = OngletNavigation.RDV,
                    onRdvClick = onRdvClick,
                    couleurActive = MaterialTheme.colorScheme.secondary,
                    couleurInactive = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            floatingActionButton = {
                BoutonTriageIA(onClick = viewModel::triageIA)
            },
        ) { contentPadding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(contentPadding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    BanniereUrgence(
                        surAppel = {
                            appelerNumero(contexte, NUMERO_URGENCE, viewModel::afficherMessage)
                        },
                    )
                }
                item {
                    SegmentedControl(
                        ongletActif = state.ongletActif,
                        nombreAVenir = state.nombreAVenir,
                        surSelection = viewModel::selectionnerOnglet,
                    )
                }

                when (state.etat) {
                    EtatListeRendezVous.CHARGEMENT -> {
                        item {
                            Box(
                                Modifier.fillMaxWidth().padding(48.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }

                    EtatListeRendezVous.ERREUR -> {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text(
                                    text = state.errorMessage ?: "Impossible de charger les rendez-vous.",
                                    style = ChatTypography.bodyMd,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center,
                                )
                                Button(
                                    onClick = { viewModel.charger() },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary,
                                        contentColor = MaterialTheme.colorScheme.onSecondary,
                                    ),
                                ) {
                                    Text("Réessayer", style = ChatTypography.labelLg)
                                }
                            }
                        }
                    }

                    EtatListeRendezVous.VIDE, EtatListeRendezVous.DONNEES -> {
                        when (state.ongletActif) {
                            OngletRendezVous.A_VENIR -> {
                                val prochain = state.prochainRendezVous
                                if (prochain != null) {
                                    item {
                                        EnteteProchainRendezVous(state.delaiProchainRendezVous)
                                    }
                                    item {
                                        CarteFeatured(
                                            rendezVous = prochain,
                                            surReprogrammer = { onReprogrammer(prochain) },
                                            surAppelerSecretariat = {
                                                appelerNumero(
                                                    contexte,
                                                    NUMERO_SECRETARIAT,
                                                    viewModel::afficherMessage,
                                                )
                                            },
                                            surAnnuler = { viewModel.demanderAnnulation(prochain) },
                                            surItineraire = { adresse ->
                                                ouvrirItineraire(contexte, adresse, viewModel::afficherMessage)
                                            },
                                        )
                                    }
                                } else {
                                    item { EtatVide("Aucun rendez-vous à venir pour le moment.") }
                                }
                                items(state.autresAVenir, key = { it.id }) { rendezVous ->
                                    CarteCompacte(
                                        rendezVous = rendezVous,
                                        surReprogrammer = { onReprogrammer(rendezVous) },
                                        surAnnuler = { viewModel.demanderAnnulation(rendezVous) },
                                        surRelancer = { viewModel.relancer(rendezVous) },
                                    )
                                }
                                item {
                                    CarteReservation(
                                        reservation = state.reservation,
                                        surSpecialite = viewModel::selectionnerSpecialite,
                                        surDeplacerCalendrier = viewModel::deplacerCalendrier,
                                        surJour = viewModel::selectionnerJour,
                                        surCreneau = viewModel::selectionnerCreneau,
                                        surConfirmer = viewModel::confirmerReservation,
                                    )
                                }
                            }

                            OngletRendezVous.PASSES -> {
                                if (state.rendezVousFiltres.isEmpty()) {
                                    item { EtatVide("Aucun rendez-vous dans votre historique.") }
                                } else {
                                    items(state.rendezVousFiltres, key = { it.id }) { rendezVous ->
                                        CarteHistorique(
                                            rendezVous = rendezVous,
                                            surCompteRendu = { viewModel.telechargerCompteRendu(rendezVous) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        state.rendezVousACancel?.let { cible ->
            CancelAppointmentDialog(
                rendezVous = cible,
                operationEnCours = state.operationEnCours,
                onConfirmer = viewModel::confirmerAnnulation,
                onAnnuler = viewModel::fermerDialogAnnulation,
            )
        }
    }
}

/** Logo mascotte 40dp réutilisé de la feature messagerie. */
@Composable
private fun ImageMascotte() {
    androidx.compose.foundation.Image(
        painter = painterResource(R.drawable.ic_copilot_mascot),
        contentDescription = "Logo Clinic Copilot",
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape),
    )
}

/** Bannière d'urgence toujours visible, avec appel implicite vers le 15. */
@Composable
private fun BanniereUrgence(surAppel: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppointmentColors.errorContainer40)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // TODO: equivalent Compose le plus proche de l'icône « emergency » du design.
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.error),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onError,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = buildAnnotatedString {
                append("En cas d'urgence vitale, composez directement le ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("15") }
                append(" ou le ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("112") }
                append(".")
            },
            style = ChatTypography.bodySm,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.error,
            shadowElevation = 0.dp,
        ) {
            Row(
                modifier = Modifier
                    .height(36.dp)
                    .clip(CircleShape)
                    .clickable(onClick = surAppel)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Appeler le 15",
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = "15",
                    style = ChatTypography.labelLg,
                    color = MaterialTheme.colorScheme.onError,
                )
            }
        }
    }
}

/** Segmented control à deux entrées, avec compteur dynamique sur « À venir ». */
@Composable
private fun SegmentedControl(
    ongletActif: OngletRendezVous,
    nombreAVenir: Int,
    surSelection: (OngletRendezVous) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = AppointmentColors.surfaceContainerLow,
        shadowElevation = 0.dp,
    ) {
        Row(Modifier.padding(4.dp)) {
            Segement(
                libelle = "À venir ($nombreAVenir)",
                icone = Icons.Default.Event,
                actif = ongletActif == OngletRendezVous.A_VENIR,
                modifier = Modifier.weight(1f),
                onClick = { surSelection(OngletRendezVous.A_VENIR) },
            )
            Segement(
                libelle = "Passés & Historique",
                icone = Icons.Default.History,
                actif = ongletActif == OngletRendezVous.PASSES,
                modifier = Modifier.weight(1f),
                onClick = { surSelection(OngletRendezVous.PASSES) },
            )
        }
    }
}

/** Une entrée du segmented control. */
@Composable
private fun Segement(
    libelle: String,
    icone: ImageVector,
    actif: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val forme = RoundedCornerShape(8.dp)
    Surface(
        modifier = modifier
            .height(44.dp)
            .clip(forme)
            .clickable(onClick = onClick)
            .then(
                if (actif) {
                    Modifier.shadow(
                        elevation = 4.dp,
                        shape = forme,
                        ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    )
                } else {
                    Modifier
                },
            ),
        shape = forme,
        color = if (actif) AppointmentColors.surfaceContainerLowest else Color.Transparent,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icone,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (actif) {
                    MaterialTheme.colorScheme.onSecondaryFixed
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = libelle,
                style = ChatTypography.labelMd,
                color = if (actif) {
                    MaterialTheme.colorScheme.onSecondaryFixed
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

/** En-tête « Prochain Rendez-vous » avec point animé et badge de délai. */
@Composable
private fun EnteteProchainRendezVous(delai: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        PointPulse()
        Spacer(Modifier.width(8.dp))
        Text(
            text = "PROCHAIN RENDEZ-VOUS",
            style = ChatTypography.labelSm,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        Spacer(Modifier.weight(1f))
        Surface(
            shape = RoundedCornerShape(50),
            color = AppointmentColors.surfaceContainerHigh,
            shadowElevation = 0.dp,
        ) {
            Text(
                text = delai,
                style = ChatTypography.labelMd,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            )
        }
    }
}

/** Petit point qui pulse, animé en boucle. */
@Composable
private fun PointPulse() {
    val transition = rememberInfiniteTransition(label = "pulse-prochain-rdv")
    val echelle by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "echelle-point",
    )
    Box(
        modifier = Modifier
            .size(8.dp)
            .alpha(echelle.coerceIn(0.6f, 1f))
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onTertiaryContainer),
    )
}

/** Carte principale du rendez-vous le plus proche. */
@Composable
private fun CarteFeatured(
    rendezVous: Appointment,
    surReprogrammer: () -> Unit,
    surAppelerSecretariat: () -> Unit,
    surAnnuler: () -> Unit,
    surItineraire: (String) -> Unit,
) {
    val forme = RoundedCornerShape(12.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = forme,
                ambientColor = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.22f),
                spotColor = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.18f),
            ),
        shape = forme,
        color = AppointmentColors.surfaceContainerLowest,
        shadowElevation = 0.dp,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box {
                    Avatar(rendezVous.doctorName, 56.dp)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Profil vérifié",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = rendezVous.doctorName,
                            style = ChatTypography.headlineSm,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Médecin vérifié",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Text(
                        text = rendezVous.specialite,
                        style = ChatTypography.bodySm,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = AppointmentColors.amber500,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${rendezVous.note} (${rendezVous.nombreAvis} avis)",
                            style = ChatTypography.labelMd,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                BadgeStatut(rendezVous.statut)
            }

            BlocDetails(rendezVous = rendezVous, surItineraire = surItineraire)
            EncartCopilot(rendezVous.doctorName)

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionRapide(
                    libelle = "Reprogrammer",
                    icone = Icons.Default.EditCalendar,
                    modifier = Modifier.weight(1f),
                    onClick = surReprogrammer,
                )
                ActionRapide(
                    libelle = "Secrétariat",
                    icone = Icons.Default.Call,
                    modifier = Modifier.weight(1f),
                    onClick = surAppelerSecretariat,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = surAnnuler)
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Cancel,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Annuler ce rendez-vous",
                    style = ChatTypography.labelMd,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

/** Bloc interne « Date et heure » + « Lieu » de la carte featured. */
@Composable
private fun BlocDetails(rendezVous: Appointment, surItineraire: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppointmentColors.surfaceContainerLow)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BoiteIcone(Icons.Default.CalendarMonth)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = "Date et heure",
                    style = ChatTypography.labelSm,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${AppointmentFormatters.formaterJourMois(rendezVous.date)} à " +
                        AppointmentFormatters.formaterHeureSeule(rendezVous.heureDebut),
                    style = ChatTypography.labelLg,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            BoiteIcone(Icons.Default.LocationOn)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = rendezVous.lieu.orEmpty().ifBlank { "Lieu à confirmer" },
                    style = ChatTypography.labelLg,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = rendezVous.adresse?.takeIf { it.isNotBlank() }
                        ?: "Lien de téléconsultation disponible avant le rendez-vous",
                    style = ChatTypography.bodySm,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            rendezVous.adresse?.takeIf { it.isNotBlank() }?.let { adresse ->
                IconButton(
                    onClick = { surItineraire(adresse) },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                ) {
                    Icon(
                        imageVector = Icons.Default.Directions,
                        contentDescription = "Ouvrir l'itinéraire",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

/** Petit carré arrondi contenant une icône de détail. */
@Composable
private fun BoiteIcone(icone: ImageVector) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(AppointmentColors.surfaceContainerLowest),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icone,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** Encart « Copilot » avec conseil de préparation IA. */
@Composable
private fun EncartCopilot(nomMedecin: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppointmentColors.surfaceContainerHigh.copy(alpha = 0.6f))
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onTertiaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Psychology,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = "Copilot",
                style = ChatTypography.labelSm,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Text(
                text = "Préparation IA : apportez vos derniers résultats et préparez vos questions " +
                    "pour $nomMedecin.",
                style = ChatTypography.bodySm,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/** Action rapide en grille 2 colonnes. */
@Composable
private fun ActionRapide(
    libelle: String,
    icone: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AppointmentColors.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.primary,
        ),
    ) {
        Icon(imageVector = icone, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(text = libelle, style = ChatTypography.labelMd)
    }
}

/** Avatar circulaire à initiale, utilisé sur toutes les cartes. */
@Composable
private fun Avatar(nom: String, taille: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(taille)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = AppointmentFormatters.initiale(nom),
            style = ChatTypography.titleMd,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

/** Badge de statut avec pastille, conforme aux couleurs utilitaires du design. */
@Composable
private fun BadgeStatut(statut: StatutRendezVous) {
    val couple = when (statut) {
        StatutRendezVous.CONFIRME -> Triple(
            AppointmentColors.emerald50,
            AppointmentColors.emerald700,
            "Confirmé",
        )
        StatutRendezVous.EN_ATTENTE -> Triple(
            AppointmentColors.amber50,
            AppointmentColors.amber800,
            "En attente",
        )
        StatutRendezVous.TERMINE -> Triple(
            AppointmentColors.surfaceContainer,
            MaterialTheme.colorScheme.primary,
            "Effectué",
        )
        StatutRendezVous.ANNULE -> Triple(
            AppointmentColors.errorContainer40,
            MaterialTheme.colorScheme.error,
            "Annulé",
        )
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(couple.first)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (statut == StatutRendezVous.CONFIRME) AppointmentColors.emerald500 else couple.second),
        )
        Text(text = couple.third, style = ChatTypography.labelSm, color = couple.second)
    }
}

/** Carte compacte d'un rendez-vous actif autre que le plus proche. */
@Composable
private fun CarteCompacte(
    rendezVous: Appointment,
    surReprogrammer: () -> Unit,
    surAnnuler: () -> Unit,
    surRelancer: () -> Unit,
) {
    val forme = RoundedCornerShape(12.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = forme,
                ambientColor = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.12f),
                spotColor = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.12f),
            ),
        shape = forme,
        color = AppointmentColors.surfaceContainerLowest,
        shadowElevation = 0.dp,
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Avatar(rendezVous.doctorName, 48.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = rendezVous.doctorName,
                        style = ChatTypography.titleMd,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = rendezVous.specialite,
                        style = ChatTypography.bodySm,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                BadgeStatut(rendezVous.statut)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "${AppointmentFormatters.formaterJourMois(rendezVous.date)} · " +
                        AppointmentFormatters.formaterHeureSeule(rendezVous.heureDebut),
                    style = ChatTypography.labelMd,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (rendezVous.statut == StatutRendezVous.EN_ATTENTE) {
                    ActionCompacte(
                        libelle = "Relancer",
                        icone = Icons.Default.NotificationImportant,
                        modifier = Modifier.weight(1f),
                        onClick = surRelancer,
                        erreur = false,
                    )
                    ActionCompacte(
                        libelle = "Retirer",
                        icone = Icons.Default.Cancel,
                        modifier = Modifier.weight(1f),
                        onClick = surAnnuler,
                        erreur = true,
                    )
                } else {
                    ActionCompacte(
                        libelle = "Reprogrammer",
                        icone = Icons.Default.EditCalendar,
                        modifier = Modifier.weight(1f),
                        onClick = surReprogrammer,
                        erreur = false,
                    )
                    ActionCompacte(
                        libelle = "Annuler",
                        icone = Icons.Default.Cancel,
                        modifier = Modifier.weight(1f),
                        onClick = surAnnuler,
                        erreur = true,
                    )
                }
            }
        }
    }
}

/** Bouton secondaire des cartes compactes. */
@Composable
private fun ActionCompacte(
    libelle: String,
    icone: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    erreur: Boolean,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (erreur) {
                AppointmentColors.errorContainer40
            } else {
                AppointmentColors.surfaceContainerLow
            },
            contentColor = if (erreur) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            },
        ),
    ) {
        Icon(imageVector = icone, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(text = libelle, style = ChatTypography.labelSm)
    }
}

/** Carte intégrée « Prendre un rendez-vous » : filtres, calendrier, créneaux, confirmation. */
@Composable
private fun CarteReservation(
    reservation: ReservationUiState,
    surSpecialite: (SpecialiteRendezVous) -> Unit,
    surDeplacerCalendrier: (Int) -> Unit,
    surJour: (Long) -> Unit,
    surCreneau: (TimeSlot) -> Unit,
    surConfirmer: () -> Unit,
) {
    val forme = RoundedCornerShape(12.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = forme,
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
            ),
        shape = forme,
        color = AppointmentColors.surfaceContainerLowest,
        shadowElevation = 0.dp,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "NOUVEAU CRÉNEAU",
                        style = ChatTypography.labelSm,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                    Text(
                        text = "Prendre un rendez-vous",
                        style = ChatTypography.headlineSm,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(AppointmentColors.surfaceContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(SpecialiteRendezVous.entries, key = { it.name }) { specialite ->
                    ChipSpecialite(
                        specialite = specialite,
                        active = specialite == reservation.specialite,
                        onClick = { surSpecialite(specialite) },
                    )
                }
            }

            CalendrierMois(
                premierJourMois = reservation.premierJourMois,
                jourSelectionne = reservation.jourSelectionne,
                surDeplacer = surDeplacerCalendrier,
                surJour = surJour,
            )

            GrilleCreneaux(
                reservation = reservation,
                surCreneau = surCreneau,
            )

            Button(
                onClick = surConfirmer,
                enabled = reservation.creneauSelectionne != null && !reservation.chargementReservation,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    disabledContainerColor = AppointmentColors.surfaceContainer,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                if (reservation.chargementReservation) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onSecondary,
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                } else {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text("Confirmer la réservation", style = ChatTypography.labelLg)
            }
        }
    }
}

/** Icône associée à une spécialité du filtre. */
private fun iconeSpecialite(specialite: SpecialiteRendezVous): ImageVector = when (specialite) {
    // TODO: pas d'icône « cardiology » exacte dans Material Icons ; local_hospital est le meilleur visuel.
    SpecialiteRendezVous.CARDIOLOGIE -> Icons.Default.LocalHospital
    SpecialiteRendezVous.GENERALISTE -> Icons.Default.MedicalServices
    SpecialiteRendezVous.PEDIATRIE -> Icons.Default.ChildCare
}

/** Chip de spécialité, actif en secondary. */
@Composable
private fun ChipSpecialite(
    specialite: SpecialiteRendezVous,
    active: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(50),
        color = if (active) {
            MaterialTheme.colorScheme.secondary
        } else {
            AppointmentColors.surfaceContainerLow
        },
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = iconeSpecialite(specialite),
                contentDescription = null,
                tint = if (active) {
                    MaterialTheme.colorScheme.onSecondary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = specialite.libelle,
                style = ChatTypography.labelMd,
                color = if (active) {
                    MaterialTheme.colorScheme.onSecondary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

/** Mois du calendrier avec navigation et en-têtes de jours. */
@Composable
private fun CalendrierMois(
    premierJourMois: Long,
    jourSelectionne: Long,
    surDeplacer: (Int) -> Unit,
    surJour: (Long) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = AppointmentFormatters.formaterMoisAnnee(premierJourMois),
                style = ChatTypography.titleMd,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { surDeplacer(-1) }) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Mois précédent",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            IconButton(onClick = { surDeplacer(1) }) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Mois suivant",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Row {
            JOURS_SEMAINE.forEach { jour ->
                Text(
                    text = jour,
                    style = ChatTypography.labelSm,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        AppointmentFormatters.grilleMois(premierJourMois).chunked(7).forEach { semaine ->
            Row(modifier = Modifier.fillMaxWidth()) {
                semaine.forEach { jour ->
                    CaseJour(
                        numero = jour.numero,
                        selectionne = jour.appartientAuMois && jour.horodatage == jourSelectionne,
                        cliquable = jour.appartientAuMois,
                        onClick = { surJour(jour.horodatage) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/** Une case de la grille mensuelle : ronde, avec un point sous le jour sélectionné. */
@Composable
private fun CaseJour(
    numero: Int,
    selectionne: Boolean,
    cliquable: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.height(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(
                    when {
                        !cliquable -> Color.Transparent
                        selectionne -> MaterialTheme.colorScheme.secondary
                        else -> AppointmentColors.surfaceContainerLow
                    },
                )
                .then(if (cliquable) Modifier.clickable(onClick = onClick) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = numero.toString(),
                style = ChatTypography.labelMd,
                color = when {
                    !cliquable -> MaterialTheme.colorScheme.outlineVariant
                    selectionne -> MaterialTheme.colorScheme.onSecondary
                    else -> MaterialTheme.colorScheme.onSurface
                },
            )
        }
        Spacer(Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(
                    if (selectionne) {
                        MaterialTheme.colorScheme.onSecondary
                    } else {
                        Color.Transparent
                    },
                ),
        )
    }
}

/** Groupes « Matin » et « Après-midi » de la grille de créneaux. */
@Composable
private fun GrilleCreneaux(
    reservation: ReservationUiState,
    surCreneau: (TimeSlot) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        when (reservation.etat) {
            EtatCreneauxRendezVous.CHARGEMENT -> {
                Box(Modifier.fillMaxWidth().height(72.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                }
            }
            EtatCreneauxRendezVous.VIDE -> {
                Text(
                    text = if (reservation.jourSelectionne == 0L) {
                        "Sélectionnez une date pour afficher les créneaux disponibles."
                    } else {
                        "Aucun créneau disponible pour cette date."
                    },
                    style = ChatTypography.bodySm,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            EtatCreneauxRendezVous.ERREUR -> {
                Text(
                    text = "Impossible de charger les créneaux. Réessayez avec un autre jour.",
                    style = ChatTypography.bodySm,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            EtatCreneauxRendezVous.DONNEES -> {
                GroupeCreneaux(
                    titre = "Matin",
                    icone = Icons.Default.WbSunny,
                    creneaux = reservation.creneauxMatin,
                    selectionne = reservation.creneauSelectionne,
                    surCreneau = surCreneau,
                )
                // TODO: wb_twilight n'est pas garanti selon les versions du set étendu ;
                // Brightness3 est le meilleur équivalent visuel si l'icône manque.
                GroupeCreneaux(
                    titre = "Après-midi",
                    icone = Icons.Default.Brightness3,
                    creneaux = reservation.creneauxApresMidi,
                    selectionne = reservation.creneauSelectionne,
                    surCreneau = surCreneau,
                )
            }
        }
    }
}

/** Un groupe de créneaux affichés en grille deux colonnes. */
@Composable
private fun GroupeCreneaux(
    titre: String,
    icone: ImageVector,
    creneaux: List<TimeSlot>,
    selectionne: TimeSlot?,
    surCreneau: (TimeSlot) -> Unit,
) {
    if (creneaux.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icone,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = titre,
                style = ChatTypography.labelMd,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        creneaux.chunked(2).forEach { paire ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                paire.forEach { creneau ->
                    ChipCreneau(
                        creneau = creneau,
                        selectionne = creneau.id == selectionne?.id,
                        modifier = Modifier.weight(1f),
                        onClick = { surCreneau(creneau) },
                    )
                }
                if (paire.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** Chip pill d'un créneau, désactivé visuellement s'il est déjà occupé. */
@Composable
private fun ChipCreneau(
    creneau: TimeSlot,
    selectionne: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val forme = RoundedCornerShape(50)
    Surface(
        modifier = modifier
            .height(36.dp)
            .alpha(if (creneau.disponible) 1f else 0.4f)
            .clip(forme)
            .clickable(enabled = creneau.disponible, onClick = onClick)
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
            ),
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
                text = AppointmentFormatters.formaterHeureSeule(creneau.heureDebut),
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

/** Carte simplifiée de l'historique, avec compte-rendu quand il est disponible. */
@Composable
private fun CarteHistorique(rendezVous: Appointment, surCompteRendu: () -> Unit) {
    val annule = rendezVous.statut == StatutRendezVous.ANNULE
    val forme = RoundedCornerShape(12.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (annule) 0.7f else 1f),
        shape = forme,
        color = if (annule) {
            AppointmentColors.errorContainer40
        } else {
            AppointmentColors.surfaceContainerLowest
        },
        shadowElevation = 0.dp,
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Avatar(rendezVous.doctorName, 48.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = rendezVous.doctorName,
                        style = ChatTypography.titleMd,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = rendezVous.specialite,
                        style = ChatTypography.bodySm,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                BadgeStatut(rendezVous.statut)
            }
            Text(
                text = AppointmentFormatters.formaterDateComplete(rendezVous.date),
                style = ChatTypography.labelLg,
                color = MaterialTheme.colorScheme.primary,
            )
            if (rendezVous.compteRenduDisponible && !annule) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Compte-rendu disponible",
                        style = ChatTypography.labelMd,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = surCompteRendu,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppointmentColors.surfaceContainerLow,
                            contentColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Text("Télécharger", style = ChatTypography.labelSm)
                    }
                }
            }
        }
    }
}

/** Message explicite quand un onglet ne contient aucun rendez-vous. */
@Composable
private fun EtatVide(message: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Default.CalendarMonth,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = message,
            style = ChatTypography.bodyMd,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** Bouton flottant « Triage IA » en dégradé, au-dessus de la navigation basse. */
@Composable
private fun BoutonTriageIA(onClick: () -> Unit) {
    val forme = RoundedCornerShape(50)
    Box(
        modifier = Modifier
            .shadow(
                elevation = 10.dp,
                shape = forme,
                ambientColor = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.4f),
                spotColor = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.3f),
            )
            .clip(forme)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        MaterialTheme.colorScheme.secondary,
                        MaterialTheme.colorScheme.onTertiaryContainer,
                    ),
                ),
            )
            .clickable(onClick = onClick)
            .height(46.dp)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.SmartToy,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Triage IA",
                style = ChatTypography.labelMd,
                color = MaterialTheme.colorScheme.onSecondary,
            )
        }
    }
}

/** Lance l'action DIAL vers un numéro, avec un retour Snackbar si aucune app n'est disponible. */
private fun appelerNumero(contexte: Context, numero: String, surErreur: (String) -> Unit) {
    val intention = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$numero"))
    try {
        contexte.startActivity(intention)
    } catch (_: ActivityNotFoundException) {
        surErreur("Aucune application téléphonique n'est disponible.")
    }
}

/** Ouvre Google Maps sur l'adresse demandée, avec un retour Snackbar en cas d'absence. */
private fun ouvrirItineraire(contexte: Context, adresse: String, surErreur: (String) -> Unit) {
    val intention = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("https://maps.google.com/?q=${Uri.encode(adresse)}"),
    )
    try {
        contexte.startActivity(intention)
    } catch (_: ActivityNotFoundException) {
        surErreur("Aucune application de cartographie n'est disponible.")
    }
}

private const val NUMERO_URGENCE = "15"
private const val NUMERO_SECRETARIAT = "+33145678900"
private val JOURS_SEMAINE = listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim")
