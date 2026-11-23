package com.clinic.copilot.feature.profile.ui

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clinic.copilot.feature.home.ui.HomeBottomBar
import com.clinic.copilot.feature.profile.domain.PatientProfile
import com.clinic.copilot.ui.theme.ChatColors

/**
 * Écran Profil Patient – Membre 2 (Sprint 6, US-13).
 *
 * Conforme à la 4ème maquette : Thomas Dubois, N° Sécu masqué,
 * carte Vitale vérifiée, Santé & données médicales, Confidentialité & Triage IA,
 * Préférences & déconnexion.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onDeconnexion: () -> Unit,
    onAccueil: () -> Unit = {},
    onMedecins: () -> Unit = {},
    onRdv: () -> Unit = {},
    onMessages: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.estDeconnecte) {
        if (uiState.estDeconnecte) {
            onDeconnexion()
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
                            Text("Mon Espace Patient", fontSize = 11.sp, color = ChatColors.textSecondary)
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
                selected = "Profil",
                onMessages = onMessages,
                onMedecins = onMedecins,
                onRdv = onRdv,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Carte Patient Header ──────────────────────────────────────
            item { PatientHeaderCard(uiState.profile) }

            // ── Carte Assistant Copilot Actif ─────────────────────────────
            item { AssistantCopilotBanner() }

            // ── Section Santé & Données médicales ─────────────────────────
            item { SanteDonneesSection(uiState.profile) }

            // ── Section Confidentialité & Triage IA ───────────────────────
            item {
                ConfidentialiteSection(
                    profile = uiState.profile,
                    onToggleTriage = viewModel::toggleConsentementTriageIA,
                )
            }

            // ── Section Préférences & Notifications ───────────────────────
            item {
                PreferencesSection(
                    profile = uiState.profile,
                    onToggleRappels = viewModel::toggleRappelsRdv,
                )
            }

            // ── Bouton Déconnexion ────────────────────────────────────────
            item {
                Button(
                    onClick = viewModel::seDeconnecter,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ChatColors.urgenceCritique.copy(alpha = 0.1f),
                        contentColor = ChatColors.urgenceCritique,
                    ),
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Se déconnecter", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Composants privés
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PatientHeaderCard(profile: PatientProfile) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ChatColors.cardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(ChatColors.primaryBlue.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = ChatColors.primaryBlue, modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${profile.nomComplet}, ${profile.age} ans",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = ChatColors.primaryNavy,
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Default.Edit, contentDescription = "Modifier", tint = ChatColors.primaryBlue, modifier = Modifier.size(14.dp))
                    }
                    Text(
                        text = "N° Sécu : ${profile.numeroSecuMasque}",
                        fontSize = 12.sp,
                        color = ChatColors.textSecondary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = ChatColors.validationSucces, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Dossier vérifié Carte Vitale",
                            fontSize = 11.sp,
                            color = ChatColors.validationSucces,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Divider(color = Color(0xFFF1F5F9))
            Spacer(Modifier.height(12.dp))

            // 3 compteurs statistiques
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                StatItem(nombre = profile.consultationsCount.toString(), label = "Consultations")
                StatItem(nombre = profile.traitementsCount.toString(), label = "Traitements")
                StatItem(nombre = profile.medecinReferentCount.toString(), label = "Médecin réf.")
            }
        }
    }
}

@Composable
private fun StatItem(nombre: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(nombre, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = ChatColors.primaryNavy)
        Text(label, fontSize = 11.sp, color = ChatColors.textSecondary)
    }
}

@Composable
private fun AssistantCopilotBanner() {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ChatColors.primaryNavy),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ChatColors.turquoiseAccent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = ChatColors.turquoiseAccent, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Assistant Copilot actif", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                Text("Surveillance de traitement & rappels personnalisés", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(ChatColors.primaryBlue)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text("Aperçu >", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SanteDonneesSection(profile: PatientProfile) {
    SectionCard(titre = "Santé & Données médicales", badge = "3 éléments") {
        ItemLigne(
            icon = Icons.Default.Badge,
            titre = "Infos personnelles & N° sécu",
            sousTitre = "Données d'identité certifiées FranceConnect",
        )
        Divider(color = Color(0xFFF1F5F9))
        ItemLigne(
            icon = Icons.Default.MedicalServices,
            titre = "Médecin traitant déclaré",
            sousTitre = "${profile.medecinTraitantNom} · ${profile.medecinTraitantSpecialite}",
            badgeTag = "Actif",
            badgeColor = ChatColors.primaryBlue,
        )
        Divider(color = Color(0xFFF1F5F9))
        ItemLigne(
            icon = Icons.Default.Security,
            titre = "Mutuelle & Tiers payant",
            sousTitre = "${profile.mutuelleNom} · Validité ${profile.mutuelleValidite}",
            badgeTag = "À jour",
            badgeColor = ChatColors.validationSucces,
        )
    }
}

@Composable
private fun ConfidentialiteSection(
    profile: PatientProfile,
    onToggleTriage: (Boolean) -> Unit,
) {
    SectionCard(titre = "Confidentialité & Triage IA", badge = "HDS France") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Consentement triage IA", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = ChatColors.primaryNavy)
                Text(
                    "Analyse algorithmique préalable de vos symptômes pour orienter vos rendez-vous",
                    fontSize = 11.sp,
                    color = ChatColors.textSecondary,
                )
            }
            Spacer(Modifier.width(8.dp))
            Switch(
                checked = profile.consentementTriageIA,
                onCheckedChange = onToggleTriage,
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = ChatColors.primaryBlue),
            )
        }
        Divider(color = Color(0xFFF1F5F9))
        ItemLigne(
            icon = Icons.Default.Shield,
            titre = "Hébergement Données de Santé",
            sousTitre = "Certifié HDS France · Conformité RGPD niveau...",
        )
        Divider(color = Color(0xFFF1F5F9))
        ItemLigne(
            icon = Icons.Default.Person,
            titre = "Partages praticiens",
            sousTitre = "${profile.praticiensPartagesCount} praticiens autorisés à consulter vos bilans",
        )
    }
}

@Composable
private fun PreferencesSection(
    profile: PatientProfile,
    onToggleRappels: (Boolean) -> Unit,
) {
    SectionCard(titre = "Préférences & Notifications") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Rappels de rendez-vous", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = ChatColors.primaryNavy)
                Text("SMS & Push 24h avant et le matin même", fontSize = 11.sp, color = ChatColors.textSecondary)
            }
            Spacer(Modifier.width(8.dp))
            Switch(
                checked = profile.rappelsRdvActifs,
                onCheckedChange = onToggleRappels,
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = ChatColors.primaryBlue),
            )
        }
    }
}

@Composable
private fun SectionCard(
    titre: String,
    badge: String? = null,
    content: @Composable () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ChatColors.cardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(titre, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ChatColors.primaryNavy)
                if (badge != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFEAF5FF))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(badge, fontSize = 10.sp, color = ChatColors.primaryBlue, fontWeight = FontWeight.Medium)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun ItemLigne(
    icon: ImageVector,
    titre: String,
    sousTitre: String,
    badgeTag: String? = null,
    badgeColor: Color = ChatColors.primaryBlue,
    onClick: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(ChatColors.primaryBlue.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = ChatColors.primaryBlue, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(titre, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = ChatColors.primaryNavy)
                if (badgeTag != null) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(badgeColor.copy(alpha = 0.15f))
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                    ) {
                        Text(badgeTag, fontSize = 9.sp, color = badgeColor, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Text(sousTitre, fontSize = 11.sp, color = ChatColors.textSecondary)
        }
        Icon(
            Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = ChatColors.textSecondary.copy(alpha = 0.5f),
            modifier = Modifier.size(12.dp),
        )
    }
}
