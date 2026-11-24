package com.clinic.copilot.feature.doctors.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clinic.copilot.feature.doctors.domain.Doctor
import com.clinic.copilot.feature.doctors.domain.FiltreMedecin
import com.clinic.copilot.feature.home.ui.HomeBottomBar
import com.clinic.copilot.ui.theme.ChatColors

/**
 * Écran Annuaire des Médecins – US-20 (Recherche), US-21 (Filtres), US-22 (Fiche praticien), US-31 (Réservation).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorsScreen(
    specialiteInitiale: String? = null,
    onDoctorMessageClick: (Doctor) -> Unit = {},
    onAccueil: () -> Unit = {},
    onRdv: () -> Unit = {},
    onMessages: () -> Unit = {},
    onProfil: () -> Unit = {},
    viewModel: DoctorsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(specialiteInitiale) {
        if (!specialiteInitiale.isNullOrBlank()) {
            viewModel.filtrerParSpecialite(specialiteInitiale)
        }
    }

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
                            Text("Annuaire & Praticiens", fontSize = 11.sp, color = ChatColors.textSecondary)
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // ── Barre de recherche & Puces ─────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ChatColors.cardSurface)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = viewModel::onSearchQueryChange,
                        placeholder = { Text("Nom du médecin, spécialité, ville...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ChatColors.textSecondary) },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Effacer", tint = ChatColors.textSecondary)
                                }
                            } else {
                                Icon(Icons.Default.Mic, contentDescription = "Recherche vocale", tint = ChatColors.primaryBlue)
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ChatColors.primaryBlue,
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedContainerColor = ChatColors.cardSurface,
                            unfocusedContainerColor = Color(0xFFF8FAFC),
                        ),
                    )

                    Spacer(Modifier.height(8.dp))

                    // Puces de filtre
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(FiltreMedecin.values()) { filtre ->
                            val actif = filtre == uiState.activeFilter
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (actif) ChatColors.primaryBlue else Color(0xFFEAF0FB))
                                    .clickable { viewModel.onFilterSelect(filtre) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                            ) {
                                Text(
                                    text = filtre.label,
                                    color = if (actif) Color.White else ChatColors.textMain,
                                    fontSize = 11.sp,
                                    fontWeight = if (actif) FontWeight.Bold else FontWeight.Medium,
                                )
                            }
                        }
                    }
                }

                // ── Barre d'en-tête de section avec Toggle Vue Carte / Vue Liste ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Médecins à proximité", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = ChatColors.primaryNavy)
                        Spacer(Modifier.width(6.dp))
                        Text("(${uiState.filteredDoctors.size})", fontSize = 12.sp, color = ChatColors.textSecondary)
                    }

                    // BOUTON TOGGLE VUE CARTE (US-20)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (uiState.isMapView) ChatColors.primaryNavy else Color(0xFFEAF0FB),
                        modifier = Modifier.clickable { viewModel.toggleMapView() },
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Icon(
                                if (uiState.isMapView) Icons.Default.FormatListBulleted else Icons.Default.Map,
                                contentDescription = null,
                                tint = if (uiState.isMapView) Color.White else ChatColors.primaryBlue,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (uiState.isMapView) "Vue Liste" else "Vue Carte",
                                fontSize = 12.sp,
                                color = if (uiState.isMapView) Color.White else ChatColors.primaryBlue,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }

                // ── Contenu principal : Vue Carte OU Vue Liste ────────────────
                if (uiState.isMapView) {
                    // VUE CARTE INTERACTIVE
                    InteractiveMapView(
                        doctors = uiState.filteredDoctors,
                        selectedDoctor = uiState.doctorEpingleCarte,
                        onSelectDoctor = viewModel::selectionnerMedecinCarte,
                        onOpenDetail = viewModel::ouvrirFicheMedecin,
                        onBookRdv = viewModel::ouvrirReservation,
                    )
                } else {
                    // VUE LISTE
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (uiState.showAiBanner) {
                            item { ConseilCopilotCard(onDismiss = viewModel::dismissAiBanner) }
                        }

                        if (uiState.filteredDoctors.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(40.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text("Aucun praticien ne correspond à votre recherche.", color = ChatColors.textSecondary, fontSize = 13.sp)
                                }
                            }
                        } else {
                            items(uiState.filteredDoctors) { doctor ->
                                DoctorCard(
                                    doctor = doctor,
                                    onFicheClick = { viewModel.ouvrirFicheMedecin(doctor) },
                                    onPrendreRdv = { viewModel.ouvrirReservation(doctor) },
                                )
                            }
                        }
                    }
                }
            }

            // ── Fiche détaillée d'un médecin (US-22) ──────────────────────────
            uiState.selectedDoctorForDetail?.let { doctor ->
                DoctorDetailDialog(
                    doctor = doctor,
                    onDismiss = viewModel::fermerFicheMedecin,
                    onPrendreRdv = { viewModel.ouvrirReservation(doctor) },
                    onMessage = {
                        viewModel.fermerFicheMedecin()
                        onDoctorMessageClick(doctor)
                    },
                )
            }

            // ── Modal de prise de Rendez-vous (US-31) ────────────────────────
            uiState.selectedDoctorForBooking?.let { doctor ->
                BookingDialog(
                    doctor = doctor,
                    onDismiss = viewModel::fermerReservation,
                    onConfirmer = { date, heure, motif, teleconsult ->
                        viewModel.confirmerReservation(doctor, date, heure, motif, teleconsult)
                    },
                )
            }

            // ── Modal de confirmation avec bouton vers mes RDV ──────────────
            uiState.bookingSuccessMessage?.let { msg ->
                BookingSuccessDialog(
                    message = msg,
                    onDismiss = viewModel::effacerMessageSucces,
                    onVoirMesRdv = {
                        viewModel.effacerMessageSucces()
                        onRdv()
                    },
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 1. Vue Carte Interactive (US-20)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun InteractiveMapView(
    doctors: List<Doctor>,
    selectedDoctor: Doctor?,
    onSelectDoctor: (Doctor) -> Unit,
    onOpenDetail: (Doctor) -> Unit,
    onBookRdv: (Doctor) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Dessin du fond de plan de ville médical (Canvas)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Fond carte douce
            drawRect(color = Color(0xFFF1F5F9))

            // Rues et avenues principales
            val streetColor = Color(0xFFFFFFFF)
            val borderColor = Color(0xFFE2E8F0)

            // Rue horizontale 1
            drawRect(color = streetColor, topLeft = Offset(0f, h * 0.3f), size = androidx.compose.ui.geometry.Size(w, 24f))
            drawRect(color = borderColor, topLeft = Offset(0f, h * 0.3f), size = androidx.compose.ui.geometry.Size(w, 2f))
            drawRect(color = borderColor, topLeft = Offset(0f, h * 0.3f + 22f), size = androidx.compose.ui.geometry.Size(w, 2f))

            // Rue horizontale 2
            drawRect(color = streetColor, topLeft = Offset(0f, h * 0.65f), size = androidx.compose.ui.geometry.Size(w, 28f))
            drawRect(color = borderColor, topLeft = Offset(0f, h * 0.65f), size = androidx.compose.ui.geometry.Size(w, 2f))

            // Avenue verticale 1
            drawRect(color = streetColor, topLeft = Offset(w * 0.45f, 0f), size = androidx.compose.ui.geometry.Size(26f, h))
            drawRect(color = borderColor, topLeft = Offset(w * 0.45f, 0f), size = androidx.compose.ui.geometry.Size(2f, h))

            // Avenue diagonale
            drawLine(color = streetColor, start = Offset(0f, h * 0.8f), end = Offset(w, h * 0.15f), strokeWidth = 30f)
            drawLine(color = borderColor, start = Offset(0f, h * 0.8f), end = Offset(w, h * 0.15f), strokeWidth = 2f)

            // Espaces verts (Parcs)
            drawRoundRect(
                color = Color(0xFFDCFCE7),
                topLeft = Offset(w * 0.08f, h * 0.1f),
                size = androidx.compose.ui.geometry.Size(w * 0.28f, h * 0.15f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f),
            )
            drawRoundRect(
                color = Color(0xFFDCFCE7),
                topLeft = Offset(w * 0.68f, h * 0.5f),
                size = androidx.compose.ui.geometry.Size(w * 0.25f, h * 0.2f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f),
            )

            // Fleuve / Seine douce
            val riverPath = Path().apply {
                moveTo(0f, h * 0.55f)
                cubicTo(w * 0.3f, h * 0.5f, w * 0.6f, h * 0.6f, w, h * 0.52f)
                lineTo(w, h * 0.57f)
                cubicTo(w * 0.6f, h * 0.65f, w * 0.3f, h * 0.55f, 0f, h * 0.6f)
                close()
            }
            drawPath(path = riverPath, color = Color(0xFFBAE6FD).copy(alpha = 0.7f))

            // Marqueur position utilisateur (Vous êtes ici)
            val userCenter = Offset(w * 0.5f, h * 0.48f)
            drawCircle(color = ChatColors.primaryBlue.copy(alpha = 0.2f), radius = 28f, center = userCenter)
            drawCircle(color = Color.White, radius = 9f, center = userCenter)
            drawCircle(color = ChatColors.primaryBlue, radius = 6f, center = userCenter)
        }

        // Marqueurs interactifs des médecins placés sur la carte
        doctors.forEach { doc ->
            val isSelected = doc.id == selectedDoctor?.id
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .align(
                            BiasAlignment(
                                (doc.mapX * 2f - 1f).coerceIn(-0.85f, 0.85f),
                                (doc.mapY * 2f - 1f).coerceIn(-0.85f, 0.6f),
                            ),
                        )
                        .clickable { onSelectDoctor(doc) },
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) ChatColors.primaryNavy else Color.White,
                            shadowElevation = if (isSelected) 8.dp else 4.dp,
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) ChatColors.turquoiseAccent else ChatColors.primaryBlue.copy(alpha = 0.3f),
                            ),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) ChatColors.turquoiseAccent else ChatColors.primaryBlue),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text("Dr", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.width(5.dp))
                                Column {
                                    Text(
                                        text = doc.nomComplet.substringAfter("Dr. "),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else ChatColors.primaryNavy,
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(10.dp))
                                        Spacer(Modifier.width(2.dp))
                                        Text(
                                            "${doc.note}",
                                            fontSize = 9.sp,
                                            color = if (isSelected) Color.White.copy(alpha = 0.8f) else ChatColors.textSecondary,
                                        )
                                    }
                                }
                            }
                        }
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = if (isSelected) ChatColors.primaryNavy else ChatColors.primaryBlue,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }

        // Pastille d'indication utilisateur
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.95f),
                shadowElevation = 3.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(ChatColors.primaryBlue))
                    Spacer(Modifier.width(6.dp))
                    Text("Vous êtes à Paris 15e", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = ChatColors.primaryNavy)
                }
            }
        }

        // Carte flottante de prévisualisation du médecin sélectionné en bas de carte
        selectedDoctor?.let { doc ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
            ) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = ChatColors.cardSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ChatColors.primaryBlue.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = ChatColors.primaryBlue, modifier = Modifier.size(28.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(doc.nomComplet, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ChatColors.primaryNavy)
                                Text("${doc.specialite} · ${doc.distanceKm} km", fontSize = 12.sp, color = ChatColors.textSecondary)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(12.dp))
                                    Spacer(Modifier.width(2.dp))
                                    Text("${doc.note} (${doc.nombreAvis} avis)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            IconButton(onClick = { onOpenDetail(doc) }) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Détails", tint = ChatColors.primaryBlue)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(
                                onClick = { onOpenDetail(doc) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                            ) {
                                Text("Voir la fiche", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Button(
                                onClick = { onBookRdv(doc) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ChatColors.primaryBlue),
                            ) {
                                Text("Prendre RDV", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. Carte Médecin Liste
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DoctorCard(
    doctor: Doctor,
    onFicheClick: () -> Unit,
    onPrendreRdv: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ChatColors.cardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onFicheClick),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(ChatColors.primaryBlue.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = ChatColors.primaryBlue, modifier = Modifier.size(34.dp))
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
                        Text("${doctor.note} (${doctor.nombreAvis} avis)", fontSize = 11.sp, color = ChatColors.textMain, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = ChatColors.textSecondary, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(2.dp))
                        Text("${doctor.distanceKm} km", fontSize = 11.sp, color = ChatColors.textSecondary)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Prochains créneaux + boutons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    doctor.prochainsCreneaux.take(2).forEach { creneau ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFEAF5FF))
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                        ) {
                            Text(creneau, fontSize = 11.sp, color = ChatColors.primaryBlue, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Button(
                    onClick = onPrendreRdv,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ChatColors.primaryBlue),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Text("Prendre RDV", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. Fiche Détaillée d'un Médecin (US-22)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DoctorDetailDialog(
    doctor: Doctor,
    onDismiss: () -> Unit,
    onPrendreRdv: () -> Unit,
    onMessage: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ChatColors.cardSurface,
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                // En-tête avec bouton fermer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(ChatColors.primaryBlue.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = ChatColors.primaryBlue, modifier = Modifier.size(36.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(doctor.nomComplet, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ChatColors.primaryNavy)
                            Text(doctor.specialite, fontSize = 13.sp, color = ChatColors.textSecondary)
                            Text(doctor.secteur, fontSize = 11.sp, color = ChatColors.primaryBlue, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer", tint = ChatColors.textSecondary)
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Évaluation & Tarifs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF8FAFC))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("${doctor.note} / 5", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Text("${doctor.nombreAvis} avis vérifiés", fontSize = 10.sp, color = ChatColors.textSecondary)
                    }
                    Divider(modifier = Modifier.height(28.dp).width(1.dp), color = Color(0xFFE2E8F0))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(doctor.tarif.substringBefore(" ("), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ChatColors.primaryNavy)
                        Text("Carte Vitale acceptée", fontSize = 10.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Présentation & Diplômes
                Text("À propos du praticien", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ChatColors.primaryNavy)
                Spacer(Modifier.height(4.dp))
                Text(doctor.bio, fontSize = 12.sp, color = ChatColors.textMain, lineHeight = 17.sp)

                Spacer(Modifier.height(10.dp))
                Text("Diplômes et titres", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ChatColors.primaryNavy)
                Spacer(Modifier.height(4.dp))
                Text(doctor.diplomes, fontSize = 11.sp, color = ChatColors.textSecondary)

                Spacer(Modifier.height(10.dp))
                Text("Lieu de consultation", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ChatColors.primaryNavy)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = ChatColors.primaryBlue, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("${doctor.adresse} (${doctor.distanceKm} km)", fontSize = 11.sp, color = ChatColors.textMain)
                }

                Spacer(Modifier.height(14.dp))

                // Section Avis récents (US-80)
                Text("Avis de patients récents (${doctor.avis.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ChatColors.primaryNavy)
                Spacer(Modifier.height(6.dp))
                if (doctor.avis.isEmpty()) {
                    Text("Aucun avis pour le moment.", fontSize = 11.sp, color = ChatColors.textSecondary)
                } else {
                    doctor.avis.take(2).forEach { rev ->
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(rev.auteur, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Row {
                                        repeat(rev.note) {
                                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(10.dp))
                                        }
                                    }
                                }
                                Spacer(Modifier.height(2.dp))
                                Text(rev.commentaire, fontSize = 11.sp, color = ChatColors.textMain)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Deux boutons d'action clés : Prendre RDV ET Envoyer un message
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onMessage,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Message", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = onPrendreRdv,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ChatColors.primaryBlue),
                        modifier = Modifier.weight(1.3f),
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Prendre RDV", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. Modal de Réservation d'un RDV (US-30 & US-31)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BookingDialog(
    doctor: Doctor,
    onDismiss: () -> Unit,
    onConfirmer: (date: String, heure: String, motif: String, teleconsult: Boolean) -> Unit,
) {
    val datesDispo = listOf("Aujourd'hui", "Demain", "Vendredi 28 Nov", "Lundi 1 Déc")
    var dateChoisie by remember { mutableStateOf(datesDispo.first()) }
    var heureChoisie by remember { mutableStateOf(doctor.prochainsCreneaux.firstOrNull() ?: "14:30") }
    var isTeleconsult by remember { mutableStateOf(false) }
    var motif by remember { mutableStateOf("Consultation de suivi et ordonnance") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ChatColors.cardSurface,
            shadowElevation = 16.dp,
            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("Réserver un rendez-vous", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ChatColors.primaryNavy)
                        Text(doctor.nomComplet, fontSize = 13.sp, color = ChatColors.primaryBlue, fontWeight = FontWeight.SemiBold)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer", tint = ChatColors.textSecondary)
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Choix de la date (US-30)
                Text("1. Choisir le jour", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ChatColors.primaryNavy)
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(datesDispo) { d ->
                        val sel = d == dateChoisie
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (sel) ChatColors.primaryBlue else Color(0xFFF1F5F9))
                                .clickable { dateChoisie = d }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                        ) {
                            Text(
                                d,
                                color = if (sel) Color.White else ChatColors.textMain,
                                fontSize = 11.sp,
                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Choix de l'horaire (créneaux disponibles)
                Text("2. Choisir l'heure", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ChatColors.primaryNavy)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    doctor.prochainsCreneaux.forEach { h ->
                        val sel = h == heureChoisie
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (sel) ChatColors.primaryBlue else Color(0xFFEAF5FF))
                                .clickable { heureChoisie = h }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                        ) {
                            Text(
                                h,
                                color = if (sel) Color.White else ChatColors.primaryBlue,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Mode de consultation
                Text("3. Modalité", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ChatColors.primaryNavy)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { isTeleconsult = false },
                    ) {
                        RadioButton(selected = !isTeleconsult, onClick = { isTeleconsult = false })
                        Text("Au cabinet", fontSize = 12.sp)
                    }
                    Spacer(Modifier.width(16.dp))
                    if (doctor.teleconsultation) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { isTeleconsult = true },
                        ) {
                            RadioButton(selected = isTeleconsult, onClick = { isTeleconsult = true })
                            Text("Téléconsultation", fontSize = 12.sp)
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Motif de consultation
                Text("4. Motif de consultation", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ChatColors.primaryNavy)
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = motif,
                    onValueChange = { motif = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    maxLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ChatColors.primaryBlue,
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                    ),
                )

                Spacer(Modifier.height(18.dp))

                // Bouton de confirmation (US-31)
                Button(
                    onClick = { onConfirmer(dateChoisie, heureChoisie, motif, isTeleconsult) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ChatColors.primaryBlue),
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Confirmer le rendez-vous", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. Boîte de confirmation Succès
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BookingSuccessDialog(
    message: String,
    onDismiss: () -> Unit,
    onVoirMesRdv: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ChatColors.cardSurface,
            shadowElevation = 16.dp,
            modifier = Modifier.fillMaxWidth().padding(24.dp),
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22C55E).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(34.dp))
                }
                Spacer(Modifier.height(14.dp))
                Text("Réservation Enregistrée !", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ChatColors.primaryNavy)
                Spacer(Modifier.height(6.dp))
                Text(message, fontSize = 13.sp, color = ChatColors.textSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = onVoirMesRdv,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ChatColors.primaryBlue),
                ) {
                    Text("Consulter mes rendez-vous", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Spacer(Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Continuer sur l'annuaire", fontSize = 13.sp)
                }
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
                .background(Brush.horizontalGradient(listOf(Color(0xFF0F3B7E), Color(0xFF1389A8))))
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
                Text("Besoin d'un avis spécialisé ?", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                Spacer(Modifier.height(4.dp))
                Text("Utilisez l'outil de Triage IA depuis l'accueil pour être orienté précisément vers la bonne spécialité médicale.", fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
            }
        }
    }
}
