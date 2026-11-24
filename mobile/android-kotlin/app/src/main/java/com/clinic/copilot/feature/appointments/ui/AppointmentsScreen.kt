package com.clinic.copilot.feature.appointments.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clinic.copilot.core.data.LocalRendezVous
import com.clinic.copilot.core.data.StatutRdv
import com.clinic.copilot.feature.appointments.domain.DocumentMedical
import com.clinic.copilot.feature.appointments.domain.MomentJour
import com.clinic.copilot.feature.appointments.domain.StatutPrise
import com.clinic.copilot.feature.appointments.domain.TraitementPilulier
import com.clinic.copilot.feature.home.ui.HomeBottomBar
import com.clinic.copilot.ui.theme.ChatColors

/**
 * Écran Rendez-vous, Dossier Médical et Pilulier (US-32, US-33, US-80, US-90, US-91).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentsScreen(
    onAccueil: () -> Unit = {},
    onMedecins: () -> Unit = {},
    onMessages: () -> Unit = {},
    onProfil: () -> Unit = {},
    viewModel: AppointmentsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.messageNotification) {
        uiState.messageNotification?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.effacerMessageNotification()
        }
    }

    Scaffold(
        containerColor = ChatColors.medicalBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(ChatColors.turquoiseAccent),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("CC", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Clinic Copilot", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ChatColors.primaryNavy)
                            Text("Rendez-vous & Suivi de santé", fontSize = 11.sp, color = ChatColors.textSecondary)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Notifications, contentDescription = "Alertes", tint = ChatColors.primaryNavy)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ChatColors.cardSurface),
            )
        },
        bottomBar = {
            HomeBottomBar(
                selected = "RDV",
                onAccueil = onAccueil,
                onMessages = onMessages,
                onMedecins = onMedecins,
                onRdv = {},
                onProfil = onProfil,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // ── Onglets principaux (Rendez-vous / Pilulier / Documents) ──────
            Surface(
                color = ChatColors.cardSurface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val onglets = listOf("Rendez-vous", "Pilulier du jour", "Documents")
                    onglets.forEachIndexed { index, label ->
                        val actif = index == uiState.ongletPrincipal
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (actif) ChatColors.primaryBlue else Color(0xFFEAF0FB))
                                .clickable { viewModel.changerOngletPrincipal(index) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (actif) FontWeight.Bold else FontWeight.Medium,
                                color = if (actif) Color.White else ChatColors.textSecondary,
                            )
                        }
                    }
                }
            }

            // ── Contenu selon l'onglet ──────────────────────────────────────
            when (uiState.ongletPrincipal) {
                0 -> {
                    // ONGLET 1 : RENDEZ-VOUS (US-32, US-33, US-80)
                    RendezVousSection(
                        filtreActif = uiState.filtreStatutRdv,
                        rendezVousList = uiState.filteredRendezVous,
                        onChangerFiltre = viewModel::changerFiltreStatutRdv,
                        onAnnulerRdv = viewModel::demanderAnnulation,
                        onLaisserAvis = viewModel::ouvrirDialogueAvis,
                        onPrendreNouveauRdv = onMedecins,
                    )
                }
                1 -> {
                    // ONGLET 2 : PILULIER DU JOUR (US-91)
                    PilulierSection(
                        traitements = uiState.traitements,
                        momentSelectionne = uiState.momentSelectionne,
                        onSelectMoment = viewModel::selectionnerMoment,
                        onValiderPrise = viewModel::validerPrise,
                    )
                }
                2 -> {
                    // ONGLET 3 : DOCUMENTS & ORDONNANCES (US-90)
                    DocumentsSection(documents = uiState.documents)
                }
            }
        }

        // ── Dialogue de confirmation d'annulation (US-33) ───────────────────
        uiState.rdvPourAnnulation?.let { rdv ->
            Dialog(onDismissRequest = viewModel::fermerDialogueAnnulation) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = ChatColors.cardSurface,
                    modifier = Modifier.padding(16.dp),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Annuler le rendez-vous ?", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ChatColors.primaryNavy)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Êtes-vous sûr de vouloir annuler votre consultation avec ${rdv.nomMedecin} prévue le ${rdv.dateTexte} ?",
                            fontSize = 13.sp,
                            color = ChatColors.textMain,
                        )
                        Spacer(Modifier.height(20.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            OutlinedButton(
                                onClick = viewModel::fermerDialogueAnnulation,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                            ) {
                                Text("Garder")
                            }
                            Button(
                                onClick = { viewModel.confirmerAnnulation(rdv) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ChatColors.urgenceCritique),
                            ) {
                                Text("Annuler le RDV", color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // ── Dialogue de dépôt d'avis (US-80) ────────────────────────────────
        uiState.rdvPourAvis?.let { rdv ->
            AvisDialog(
                rdv = rdv,
                onDismiss = viewModel::fermerDialogueAvis,
                onSoumettre = { note, commentaire ->
                    viewModel.soumettreAvis(rdv, note, commentaire)
                },
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 1. Section Rendez-Vous (US-32, US-33, US-80)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RendezVousSection(
    filtreActif: StatutRdv,
    rendezVousList: List<LocalRendezVous>,
    onChangerFiltre: (StatutRdv) -> Unit,
    onAnnulerRdv: (LocalRendezVous) -> Unit,
    onLaisserAvis: (LocalRendezVous) -> Unit,
    onPrendreNouveauRdv: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Puces de statut
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatutRdv.values().forEach { st ->
                    val actif = st == filtreActif
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (actif) ChatColors.primaryNavy else Color(0xFFEAF0FB))
                            .clickable { onChangerFiltre(st) }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = st.libelle,
                            color = if (actif) Color.White else ChatColors.primaryNavy,
                            fontSize = 12.sp,
                            fontWeight = if (actif) FontWeight.Bold else FontWeight.Medium,
                        )
                    }
                }
            }
        }

        if (rendezVousList.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ChatColors.cardSurface),
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            Icons.Default.EventBusy,
                            contentDescription = null,
                            tint = ChatColors.textSecondary,
                            modifier = Modifier.size(40.dp),
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Aucun rendez-vous dans cet onglet",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = ChatColors.primaryNavy,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Trouvez un praticien et réservez un créneau directement en ligne.",
                            fontSize = 12.sp,
                            color = ChatColors.textSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = onPrendreNouveauRdv,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ChatColors.primaryBlue),
                        ) {
                            Text("Prendre un rendez-vous")
                        }
                    }
                }
            }
        } else {
            items(rendezVousList) { rdv ->
                RdvCard(
                    rdv = rdv,
                    onAnnuler = { onAnnulerRdv(rdv) },
                    onAvis = { onLaisserAvis(rdv) },
                )
            }
        }
    }
}

@Composable
private fun RdvCard(
    rdv: LocalRendezVous,
    onAnnuler: () -> Unit,
    onAvis: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ChatColors.cardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Statut Badge + Mode
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val (badgeBg, badgeFg) = when (rdv.statut) {
                    StatutRdv.A_VENIR -> Color(0xFFEAF5FF) to ChatColors.primaryBlue
                    StatutRdv.PASSE -> Color(0xFFF1F5F9) to ChatColors.textSecondary
                    StatutRdv.ANNULE -> Color(0xFFFDE8E8) to ChatColors.urgenceCritique
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeBg)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(rdv.statut.libelle, fontSize = 10.sp, color = badgeFg, fontWeight = FontWeight.Bold)
                }

                if (rdv.teleconsultation) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VideoCall, contentDescription = null, tint = ChatColors.primaryBlue, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Téléconsultation", fontSize = 11.sp, color = ChatColors.primaryBlue, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Praticien & Spécialité
            Text(rdv.nomMedecin, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = ChatColors.primaryNavy)
            Text(rdv.specialite, fontSize = 12.sp, color = ChatColors.textSecondary)

            Spacer(Modifier.height(8.dp))

            // Date et Lieu
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = ChatColors.primaryBlue, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(rdv.dateTexte, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = ChatColors.primaryNavy)
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = ChatColors.textSecondary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(rdv.lieu, fontSize = 11.sp, color = ChatColors.textSecondary)
            }

            // Actions selon le statut
            when (rdv.statut) {
                StatutRdv.A_VENIR -> {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        OutlinedButton(
                            onClick = onAnnuler,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        ) {
                            Text("Annuler le RDV", fontSize = 11.sp, color = ChatColors.urgenceCritique)
                        }
                    }
                }
                StatutRdv.PASSE -> {
                    Spacer(Modifier.height(10.dp))
                    if (rdv.noteAttribuee != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Votre note :", fontSize = 11.sp, color = ChatColors.textSecondary)
                            Spacer(Modifier.width(6.dp))
                            repeat(rdv.noteAttribuee) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                            }
                            rdv.avisLaisse?.let {
                                Spacer(Modifier.width(8.dp))
                                Text("« $it »", fontSize = 11.sp, color = ChatColors.textMain)
                            }
                        }
                    } else {
                        Button(
                            onClick = onAvis,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ChatColors.primaryBlue),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 5.dp),
                        ) {
                            Icon(Icons.Default.RateReview, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Laisser un avis (US-80)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                StatutRdv.ANNULE -> {
                    // Aucun bouton nécessaire
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. Dialogue Dépôt d'Avis (US-80)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AvisDialog(
    rdv: LocalRendezVous,
    onDismiss: () -> Unit,
    onSoumettre: (note: Int, commentaire: String) -> Unit,
) {
    var note by remember { mutableIntStateOf(5) }
    var commentaire by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ChatColors.cardSurface,
            shadowElevation = 16.dp,
            modifier = Modifier.padding(16.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Noter votre consultation", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ChatColors.primaryNavy)
                Spacer(Modifier.height(4.dp))
                Text("Avec ${rdv.nomMedecin} (${rdv.specialite})", fontSize = 12.sp, color = ChatColors.textSecondary)

                Spacer(Modifier.height(16.dp))

                // Étoiles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    (1..5).forEach { etoile ->
                        IconButton(onClick = { note = etoile }) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = if (etoile <= note) Color(0xFFF59E0B) else Color(0xFFCBD5E1),
                                modifier = Modifier.size(32.dp),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Commentaire
                OutlinedTextField(
                    value = commentaire,
                    onValueChange = { commentaire = it },
                    placeholder = { Text("Partagez votre avis (écoute, ponctualité, explications...)", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    shape = RoundedCornerShape(10.dp),
                )

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Annuler")
                    }
                    Button(
                        onClick = { onSoumettre(note, commentaire.ifBlank { "Consultation très satisfaisante." }) },
                        colors = ButtonDefaults.buttonColors(containerColor = ChatColors.primaryBlue),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Publier l'avis")
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. Section Pilulier (US-91)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PilulierSection(
    traitements: List<TraitementPilulier>,
    momentSelectionne: MomentJour,
    onSelectMoment: (MomentJour) -> Unit,
    onValiderPrise: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            // Sélecteur Matin / Midi / Soir
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MomentJour.values().forEach { moment ->
                    val sel = moment == momentSelectionne
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (sel) ChatColors.primaryNavy else Color(0xFFEAF0FB))
                            .clickable { onSelectMoment(moment) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "${moment.libelle} · ${moment.heureDefaut}",
                            color = if (sel) Color.White else ChatColors.primaryNavy,
                            fontSize = 11.sp,
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                        )
                    }
                }
            }
        }

        val filtres = traitements.filter { it.momentJour == momentSelectionne }
        if (filtres.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Aucune prise programmée pour ce créneau.", color = ChatColors.textSecondary, fontSize = 12.sp)
                }
            }
        } else {
            items(filtres) { medicament ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = ChatColors.cardSurface),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFEAF5FF)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Default.Medication, contentDescription = null, tint = ChatColors.primaryBlue, modifier = Modifier.size(24.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(medicament.nom, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ChatColors.primaryNavy)
                            Text(medicament.posologie, fontSize = 11.sp, color = ChatColors.textSecondary)
                            Spacer(Modifier.height(2.dp))
                            Text("Prescrit par ${medicament.prescritPar}", fontSize = 10.sp, color = ChatColors.primaryBlue)
                        }
                        if (medicament.statutPrise == StatutPrise.PRIS) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFDCFCE7))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(12.dp))
                                    Spacer(Modifier.width(3.dp))
                                    Text("Pris", fontSize = 11.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Button(
                                onClick = { onValiderPrise(medicament.id) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ChatColors.primaryBlue),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                Text("Valider", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. Section Documents & Ordonnances (US-90)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DocumentsSection(documents: List<DocumentMedical>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(documents) { doc ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = ChatColors.cardSurface),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF1F5F9)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = ChatColors.primaryNavy, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(doc.titre, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ChatColors.primaryNavy)
                        Text("${doc.date} · ${doc.medecin}", fontSize = 11.sp, color = ChatColors.textSecondary)
                        Text(doc.tailleFichier, fontSize = 10.sp, color = ChatColors.primaryBlue)
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Download, contentDescription = "Télécharger", tint = ChatColors.primaryBlue)
                    }
                }
            }
        }
    }
}
