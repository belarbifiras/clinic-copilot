package com.clinic.copilot.feature.triage.ui

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.clinic.copilot.feature.triage.domain.NiveauSeverite
import com.clinic.copilot.feature.triage.domain.TriageMessage
import com.clinic.copilot.feature.triage.domain.TriageRole
import com.clinic.copilot.ui.theme.ChatColors

/**
 * Écran de triage IA – Membre 2 (US-70 & US-71).
 *
 * @param onRetour         Navigation retour vers l'accueil.
 * @param onAllerAnnuaire  Navigation vers l'annuaire des médecins avec pré-filtrage de spécialité.
 * @param viewModel        ViewModel injecté par Hilt.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TriageScreen(
    onRetour: () -> Unit,
    onAllerAnnuaire: (String?) -> Unit = {},
    viewModel: TriageViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    // Auto-scroll vers le bas à chaque nouveau message
    LaunchedEffect(uiState.messages.size, uiState.isTyping) {
        val target = uiState.messages.size + (if (uiState.isTyping) 1 else 0)
        if (target > 0) listState.animateScrollToItem(target - 1)
    }

    Scaffold(
        containerColor = ChatColors.medicalBackground,
        topBar = {
            Column {
                // ── Bandeau AVERTISSEMENT VITAL avec protection de barre de statut ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFDE8E8))
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(ChatColors.urgenceCritique),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "AVERTISSEMENT VITAL · L'IA ne remplace pas un médecin. En cas d'urgence grave :",
                            fontSize = 11.sp,
                            color = ChatColors.urgenceCritique,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(6.dp))
                        Button(
                            onClick = { /* appel urgence 15 */ },
                            colors = ButtonDefaults.buttonColors(containerColor = ChatColors.urgenceCritique),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text("15", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                // ── App Bar ───────────────────────────────────────────────
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = onRetour) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = ChatColors.primaryNavy)
                        }
                    },
                    title = {
                        Column {
                            Text(
                                "Triage IA Copilot",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = ChatColors.primaryNavy,
                            )
                            Text(
                                "Orientation clinique intelligente",
                                fontSize = 11.sp,
                                color = ChatColors.textSecondary,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = ChatColors.cardSurface),
                )

                // ── Carte IA certifiée ─────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ChatColors.cardSurface)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ChatColors.turquoiseAccent),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("CC", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Clinic Copilot", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ChatColors.primaryNavy)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = ChatColors.validationSucces, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("IA Certifiée Santé · Données HDS chiffrées", fontSize = 11.sp, color = ChatColors.textSecondary)
                        }
                    }
                }

                // ── Bandeau sévérité dynamique (si le dernier message IA en a une) ──
                val derniereSeverite = uiState.messages.lastOrNull { it.severite != null }?.severite
                derniereSeverite?.let { sev ->
                    BandeauSeverite(severite = sev)
                }
            }
        },
        bottomBar = {
            SaisieBar(
                saisie = uiState.saisie,
                isTyping = uiState.isTyping,
                medecinsDisponibles = uiState.medecinsDisponibles,
                creneaux = uiState.creneauxTrouves,
                onSaisieChange = viewModel::mettreAJourSaisie,
                onEnvoyer = viewModel::envoyerMessage,
                onVoirMedecins = { onAllerAnnuaire(uiState.specialiteConclue) },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(uiState.messages) { msg ->
                BulleMessage(
                    msg = msg,
                    onSuggestion = viewModel::envoyerSuggestion,
                    onAllerAnnuaire = onAllerAnnuaire,
                )
            }

            if (uiState.isTyping) {
                item { BulleDotAnimation() }
            }
        }
    }
}

@Composable
private fun BandeauSeverite(severite: NiveauSeverite) {
    val bg = when (severite) {
        NiveauSeverite.CRITIQUE -> Color(0xFFFDE8E8)
        NiveauSeverite.MODERE -> Color(0xFFFEF3C7)
        NiveauSeverite.ROUTINIER -> Color(0xFFEAF5FF)
        NiveauSeverite.ACCOMPAGNEMENT -> Color(0xFFE6FFFA)
    }
    val fg = when (severite) {
        NiveauSeverite.CRITIQUE -> ChatColors.urgenceCritique
        NiveauSeverite.MODERE -> Color(0xFFD97706)
        NiveauSeverite.ROUTINIER -> ChatColors.primaryBlue
        NiveauSeverite.ACCOMPAGNEMENT -> ChatColors.turquoiseAccent
    }
    Surface(color = bg, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Évaluation préliminaire :", fontSize = 12.sp, color = ChatColors.textSecondary)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(fg)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(severite.libelle, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun BulleMessage(
    msg: TriageMessage,
    onSuggestion: (String) -> Unit,
    onAllerAnnuaire: (String?) -> Unit,
) {
    val estUtilisateur = msg.role == TriageRole.UTILISATEUR
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (estUtilisateur) Alignment.End else Alignment.Start,
    ) {
        // Bulle principale
        Surface(
            shape = RoundedCornerShape(
                topStart = if (estUtilisateur) 16.dp else 4.dp,
                topEnd = if (estUtilisateur) 4.dp else 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp,
            ),
            color = if (estUtilisateur) ChatColors.primaryBlue else ChatColors.cardSurface,
            shadowElevation = 1.dp,
            modifier = Modifier.padding(horizontal = 4.dp),
        ) {
            Text(
                text = msg.contenu,
                fontSize = 14.sp,
                color = if (estUtilisateur) Color.White else ChatColors.textMain,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }

        // Suggestions rapides de l'IA (puces)
        if (msg.suggestions.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(msg.suggestions) { sug ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(ChatColors.lightBlueSurface)
                            .clickable { onSuggestion(sug) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(sug, fontSize = 12.sp, color = ChatColors.primaryBlue, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        // ── BOUTON D'ORIENTATION VERS L'ANNUAIRE MÉDICAL (US-71) ───────────
        if (msg.orientationActionTexte != null) {
            Spacer(Modifier.height(10.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF22C55E).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.People,
                                contentDescription = null,
                                tint = Color(0xFF15803D),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Orientation : ${msg.orientationSpecialite ?: "Spécialiste"}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF15803D),
                            )
                            msg.medecinsDisponiblesTexte?.let {
                                Text(text = it, fontSize = 11.sp, color = ChatColors.textSecondary)
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { onAllerAnnuaire(msg.orientationSpecialite) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ChatColors.primaryBlue),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = msg.orientationActionTexte,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BulleDotAnimation() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(ChatColors.cardSurface)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text("···", fontSize = 20.sp, color = ChatColors.turquoiseAccent, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SaisieBar(
    saisie: String,
    isTyping: Boolean,
    medecinsDisponibles: Int,
    creneaux: Int,
    onSaisieChange: (String) -> Unit,
    onEnvoyer: () -> Unit,
    onVoirMedecins: () -> Unit,
) {
    Surface(color = ChatColors.cardSurface, shadowElevation = 8.dp) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            // Info médecins disponibles avec clic direct
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onVoirMedecins)
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Médecins disponibles aujourd'hui",
                    fontSize = 12.sp,
                    color = ChatColors.textSecondary,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "$medecinsDisponibles médecins · $creneaux créneaux",
                        fontSize = 12.sp,
                        color = ChatColors.validationSucces,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = ChatColors.validationSucces,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            // Champ de saisie + envoi
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = saisie,
                    onValueChange = onSaisieChange,
                    placeholder = { Text("Décrivez vos symptômes...", fontSize = 13.sp) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ChatColors.primaryBlue,
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        cursorColor = ChatColors.primaryBlue,
                    ),
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = onEnvoyer,
                    enabled = saisie.isNotBlank() && !isTyping,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (saisie.isNotBlank() && !isTyping) ChatColors.primaryBlue else Color(0xFFE2E8F0)),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Envoyer",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}
