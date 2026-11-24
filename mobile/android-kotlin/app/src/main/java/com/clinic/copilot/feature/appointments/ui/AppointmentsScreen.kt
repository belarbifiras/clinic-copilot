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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clinic.copilot.feature.appointments.domain.DocumentMedical
import com.clinic.copilot.feature.appointments.domain.MomentJour
import com.clinic.copilot.feature.appointments.domain.StatutPrise
import com.clinic.copilot.feature.appointments.domain.TraitementPilulier
import com.clinic.copilot.feature.home.ui.HomeBottomBar
import com.clinic.copilot.ui.theme.ChatColors

/**
 * Écran Dossier Médical, Pilulier et Rendez-vous – Membre 4 (Sprint 6, US-30 & US-32).
 *
 * Conforme à la 5ème maquette : suivi et prévoyance, pilulier quotidien
 * segmenté (Matin/Midi/Soir), validation de prise et documents médicaux.
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

    Scaffold(
        containerColor = ChatColors.medicalBackground,
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Dossier Médical", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ChatColors.primaryNavy)
                                Spacer(Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFEAF5FF))
                                        .padding(horizontal = 4.dp, vertical = 1.dp),
                                ) {
                                    Text("Certifié HDS", fontSize = 9.sp, color = ChatColors.primaryBlue, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text("Patient : Alexandre Dufour (34 ans)", fontSize = 11.sp, color = ChatColors.textSecondary)
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // ── Onglets Traitements / Historique ──────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEAF0FB)),
                ) {
                    listOf("Traitements (${uiState.traitements.size})", "Historique & Docs").forEachIndexed { index, titre ->
                        val actif = index == uiState.ongletActif
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (actif) ChatColors.primaryBlue else Color.Transparent)
                                .clickable { viewModel.changerOnglet(index) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = titre,
                                color = if (actif) Color.White else ChatColors.textSecondary,
                                fontWeight = if (actif) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
            }

            // ── Carte COPILOT SUIVI & PRÉVOYANCE ───────────────────────────
            item { CopilotPrevoyanceCard() }

            // ── Section Mon Pilulier du jour ──────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Mon Pilulier du jour", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = ChatColors.primaryNavy)
                    Text("Aujourd'hui, 24 Oct", fontSize = 12.sp, color = ChatColors.textSecondary)
                }
            }

            // ── 3 créneaux Matin / Midi / Soir ────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MomentCard(
                        moment = MomentJour.MATIN,
                        nb = "1 médicament",
                        selectionne = uiState.momentSelectionne == MomentJour.MATIN,
                        onClick = { viewModel.selectionnerMoment(MomentJour.MATIN) },
                        modifier = Modifier.weight(1f),
                    )
                    MomentCard(
                        moment = MomentJour.MIDI,
                        nb = "1 prévu",
                        selectionne = uiState.momentSelectionne == MomentJour.MIDI,
                        onClick = { viewModel.selectionnerMoment(MomentJour.MIDI) },
                        modifier = Modifier.weight(1f),
                    )
                    MomentCard(
                        moment = MomentJour.SOIR,
                        nb = "1 prévu",
                        selectionne = uiState.momentSelectionne == MomentJour.SOIR,
                        onClick = { viewModel.selectionnerMoment(MomentJour.SOIR) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // ── Liste des traitements du pilulier ─────────────────────────
            items(uiState.traitements) { t ->
                TraitementCard(traitement = t, onValider = { viewModel.validerPrise(t.id) })
            }

            // ── Message important posologie ───────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEAF5FF))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = ChatColors.primaryBlue, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Important : Les posologies sont validées exclusivement par vos praticiens. En cas de doute, échangez via la messagerie sécurisée.",
                        fontSize = 11.sp,
                        color = ChatColors.primaryNavy,
                        lineHeight = 15.sp,
                    )
                }
            }

            // ── Section Comptes-rendus & Documents ────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Comptes-rendus & Documents", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = ChatColors.primaryNavy)
                    Text("Tout voir", fontSize = 12.sp, color = ChatColors.primaryBlue, fontWeight = FontWeight.Medium)
                }
            }

            items(uiState.documents) { doc ->
                DocumentCard(document = doc)
            }
        }
    }
}

@Composable
private fun CopilotPrevoyanceCard() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEBF6FD)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ChatColors.primaryBlue),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Medication, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("COPILOT SUIVI & PRÉVOYANCE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ChatColors.primaryNavy)
                    Text("Mis à jour à 08:30", fontSize = 10.sp, color = ChatColors.textSecondary)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Observance optimale aujourd'hui : 1 prise effectuée sur 3 prévues. Pensez à votre Doliprane à 14h00 si la céphalée persiste.",
                fontSize = 12.sp,
                color = ChatColors.primaryNavy,
                lineHeight = 16.sp,
            )
        }
    }
}

@Composable
private fun MomentCard(
    moment: MomentJour,
    nb: String,
    selectionne: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selectionne) ChatColors.primaryBlue else ChatColors.cardSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = moment.libelle,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (selectionne) Color.White else ChatColors.primaryNavy,
            )
            Text(
                text = moment.heureDefaut,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (selectionne) Color.White else ChatColors.primaryBlue,
            )
            Text(
                text = nb,
                fontSize = 10.sp,
                color = if (selectionne) Color.White.copy(alpha = 0.8f) else ChatColors.textSecondary,
            )
        }
    }
}

@Composable
private fun TraitementCard(traitement: TraitementPilulier, onValider: () -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ChatColors.cardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ChatColors.primaryBlue.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Medication, contentDescription = null, tint = ChatColors.primaryBlue, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(traitement.nom, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ChatColors.primaryNavy)
                    Text(traitement.posologie, fontSize = 11.sp, color = ChatColors.textSecondary)
                }
                if (traitement.statutPrise == StatutPrise.PRIS) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFE6FAF8))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text("Pris à ${traitement.creneauHoraire}", fontSize = 10.sp, color = ChatColors.turquoiseAccent, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFEAF5FF))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text("À prendre à ${traitement.creneauHoraire}", fontSize = 10.sp, color = ChatColors.primaryBlue, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Prescrit par : ${traitement.prescritPar} · ${traitement.progressionJours}",
                    fontSize = 11.sp,
                    color = ChatColors.textSecondary,
                )
                if (traitement.statutPrise != StatutPrise.PRIS) {
                    Button(
                        onClick = onValider,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ChatColors.primaryBlue),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    ) {
                        Text("Valider la prise", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DocumentCard(document: DocumentMedical) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ChatColors.cardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFFDE8E8)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Description, contentDescription = null, tint = ChatColors.urgenceCritique, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(document.titre, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ChatColors.primaryNavy)
                Text("${document.medecin} · ${document.date} · ${document.tailleFichier}", fontSize = 11.sp, color = ChatColors.textSecondary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = {}, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Download, contentDescription = "Télécharger", tint = ChatColors.primaryBlue, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = {}, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Visibility, contentDescription = "Consulter", tint = ChatColors.primaryNavy, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
