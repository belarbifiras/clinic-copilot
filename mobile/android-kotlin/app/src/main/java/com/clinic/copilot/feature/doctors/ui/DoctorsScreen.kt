package com.clinic.copilot.feature.doctors.ui

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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clinic.copilot.feature.doctors.domain.Doctor
import com.clinic.copilot.feature.doctors.domain.FiltreMedecin
import com.clinic.copilot.feature.home.ui.HomeBottomBar
import com.clinic.copilot.ui.theme.ChatColors

/**
 * Écran Annuaire des Médecins – Membre 3 (Sprint 6, US-20 & US-22).
 *
 * Conforme à la 6ème maquette : recherche praticien/spécialité,
 * filtres d'état, carte conseil IA contextuelle, cartes de médecins
 * avec notes, secteur et créneaux.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorsScreen(
    onDoctorClick: (Doctor) -> Unit = {},
    onAccueil: () -> Unit = {},
    onRdv: () -> Unit = {},
    onMessages: () -> Unit = {},
    onProfil: () -> Unit = {},
    viewModel: DoctorsViewModel = hiltViewModel(),
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
                            Text("Clinic Copilot", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ChatColors.primaryNavy)
                            Text("Médecins", fontSize = 11.sp, color = ChatColors.textSecondary)
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
                selected = "Médecins",
                onAccueil = onAccueil,
                onMessages = onMessages,
                onMedecins = {},
                onRdv = onRdv,
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
            // ── Barre de recherche ────────────────────────────────────────
            item {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    placeholder = { Text("Nom du médecin, spécialité, ville...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ChatColors.textSecondary) },
                    trailingIcon = { Icon(Icons.Default.Mic, contentDescription = "Recherche vocale", tint = ChatColors.primaryBlue) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ChatColors.primaryBlue,
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedContainerColor = ChatColors.cardSurface,
                        unfocusedContainerColor = ChatColors.cardSurface,
                    ),
                )
            }

            // ── Puces de filtre (LazyRow) ──────────────────────────────────
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(FiltreMedecin.values()) { filtre ->
                        val actif = filtre == uiState.activeFilter
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (actif) ChatColors.primaryBlue else Color(0xFFEAF0FB))
                                .clickable { viewModel.onFilterSelect(filtre) }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        ) {
                            Text(
                                text = filtre.label,
                                color = if (actif) Color.White else ChatColors.textMain,
                                fontSize = 12.sp,
                                fontWeight = if (actif) FontWeight.Bold else FontWeight.Medium,
                            )
                        }
                    }
                }
            }

            // ── Carte CONSEIL COPILOT IA ───────────────────────────────────
            if (uiState.showAiBanner) {
                item {
                    ConseilCopilotCard(onDismiss = viewModel::dismissAiBanner)
                }
            }

            // ── En-tête de section "Médecins à proximité" ──────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Médecins à proximité", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = ChatColors.primaryNavy)
                        Spacer(Modifier.width(6.dp))
                        Text("(${uiState.filteredDoctors.size} disponibles)", fontSize = 12.sp, color = ChatColors.textSecondary)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { /* Vue carte toggle */ },
                    ) {
                        Icon(Icons.Default.Map, contentDescription = null, tint = ChatColors.primaryBlue, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Vue Carte", fontSize = 12.sp, color = ChatColors.primaryBlue, fontWeight = FontWeight.Medium)
                    }
                }
            }

            // ── Liste des praticiens ──────────────────────────────────────
            items(uiState.filteredDoctors) { doctor ->
                DoctorCard(doctor = doctor, onClick = { onDoctorClick(doctor) })
            }
        }
    }
}

@Composable
private fun ConseilCopilotCard(onDismiss: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF0F3B7E), Color(0xFF1389A8)),
                    ),
                )
                .padding(14.dp),
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SmartToy, contentDescription = null, tint = ChatColors.turquoiseAccent, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("CONSEIL COPILOT IA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ChatColors.turquoiseAccent)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onDismiss, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Besoin d'un avis neurologique ?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Basé sur votre bilan céphalée d'hier : 3 neurologues sont disponibles près de vous cette semaine sans avance de frais.",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.85f),
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {},
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ChatColors.primaryBlue),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text("Explorer la sélection >", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                    ) {
                        Text("Créneaux vérifiés", fontSize = 11.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun DoctorCard(doctor: Doctor, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ChatColors.cardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(ChatColors.primaryBlue.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = ChatColors.primaryBlue, modifier = Modifier.size(36.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFEAF5FF))
                            .padding(horizontal = 6.dp, vertical = 1.dp),
                    ) {
                        Text(doctor.secteur, fontSize = 9.sp, color = ChatColors.primaryBlue, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(doctor.nomComplet, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = ChatColors.primaryNavy)
                    Text(doctor.specialite, fontSize = 12.sp, color = ChatColors.textSecondary)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("${doctor.note} (${doctor.nombreAvis})", fontSize = 11.sp, color = ChatColors.textMain, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = ChatColors.textSecondary, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(2.dp))
                        Text("${doctor.adresse} · ${doctor.distanceKm} km", fontSize = 11.sp, color = ChatColors.textSecondary)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Prochains créneaux
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    doctor.prochainsCreneaux.take(3).forEach { creneau ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFEAF5FF))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Text(creneau, fontSize = 11.sp, color = ChatColors.primaryBlue, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Button(
                    onClick = onClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ChatColors.primaryBlue),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text("Prendre RDV", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
