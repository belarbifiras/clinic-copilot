package com.clinic.copilot.feature.home.ui

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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clinic.copilot.feature.home.domain.HomeData
import com.clinic.copilot.feature.home.domain.MedicamentJour
import com.clinic.copilot.feature.home.domain.ProchainRdv
import com.clinic.copilot.ui.theme.ChatColors

/**
 * Écran d'accueil patient – Membre 2, écran 1/2.
 *
 * Maquette : bandeau urgence 15, salutation, barre de recherche,
 * carte Triage Copilot Santé, accès rapide (Médecins/Ordonnances/Téléconsult/Garde),
 * prochain RDV, médicaments du jour.
 *
 * @param onAllerTriage   Navigation vers l'écran de triage IA.
 * @param onAllerMessages Navigation vers la messagerie.
 * @param onAllerMedecins Navigation vers l'annuaire médecins.
 * @param onAllerRdv      Navigation vers les rendez-vous.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAllerTriage: () -> Unit,
    onAllerMessages: () -> Unit = {},
    onAllerMedecins: () -> Unit = {},
    onAllerRdv: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.naviguerVersTriage) {
        if (uiState.naviguerVersTriage) {
            viewModel.triageNavigueDone()
            onAllerTriage()
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
                            Text("Accueil", fontSize = 11.sp, color = ChatColors.textSecondary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ChatColors.cardSurface),
            )
        },
        bottomBar = { HomeBottomBar(selected = "Accueil", onMessages = onAllerMessages, onMedecins = onAllerMedecins, onRdv = onAllerRdv) },
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ChatColors.primaryBlue)
            }
            return@Scaffold
        }
        val data = uiState.homeData ?: return@Scaffold

        LazyColumn(
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // ── Bandeau urgence ───────────────────────────────────────────
            item { BandeauUrgence() }

            // ── Salutation ────────────────────────────────────────────────
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                    Text(
                        text = "Bonjour, ${data.prenomPatient} 👋",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = ChatColors.primaryNavy,
                    )
                    Text(
                        text = "Comment vous sentez-vous aujourd'hui ?",
                        fontSize = 14.sp,
                        color = ChatColors.textSecondary,
                    )
                }
            }

            // ── Carte Triage IA ───────────────────────────────────────────
            item { CarteTriage(onDemarrer = { viewModel.demanderTriage() }) }

            // ── Accès rapide ──────────────────────────────────────────────
            item { AccesRapide(onMedecins = onAllerMedecins, onRdv = onAllerRdv, onMessages = onAllerMessages) }

            // ── Prochain RDV ──────────────────────────────────────────────
            data.prochainRdv?.let { rdv ->
                item { ProchainRdvCard(rdv = rdv) }
            }

            // ── Médicaments du jour ───────────────────────────────────────
            item { MedicamentsSection(meds = data.medicamentsJour, onValider = viewModel::validerPriseMedicament) }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Composants privés
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BandeauUrgence() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(ChatColors.urgenceCritique)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "⚠ Urgence vitale ou détresse ? Appelez...",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = { /* Intent tel:15 */ },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = ChatColors.urgenceCritique),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
            ) {
                Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("15", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun CarteTriage(onDemarrer: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(ChatColors.primaryNavy, Color(0xFF1E4DB7)),
                    ),
                )
                .padding(16.dp),
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(ChatColors.turquoiseAccent),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("TRIAGE IA ACTIF", fontSize = 10.sp, color = ChatColors.turquoiseAccent, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Votre Copilot Santé",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Text(
                    text = "Décrivez vos symptômes en langage simple pour un avis...",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.8f),
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onDemarrer,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ChatColors.turquoiseAccent,
                        contentColor = Color.White,
                    ),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Icon(Icons.Default.SmartToy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Démarrer une discussion", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun AccesRapide(onMedecins: () -> Unit, onRdv: () -> Unit, onMessages: () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text("Accès rapide", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = ChatColors.primaryNavy)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
            AccesRapideItem("Médecins", Icons.Default.People, ChatColors.primaryBlue, onMedecins)
            AccesRapideItem("Ordonnances", Icons.Default.Medication, Color(0xFF38A169)) {}
            AccesRapideItem("Téléconsult", Icons.Default.ChatBubble, ChatColors.turquoiseAccent, onMessages)
            AccesRapideItem("Garde 112", Icons.Default.Phone, ChatColors.urgenceCritique) {}
        }
    }
}

@Composable
private fun AccesRapideItem(label: String, icon: ImageVector, couleur: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(couleur.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = label, tint = couleur, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 11.sp, color = ChatColors.textMain, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ProchainRdvCard(rdv: ProchainRdv) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Prochain rendez-vous", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = ChatColors.primaryNavy)
            Text("Voir l'historique", fontSize = 12.sp, color = ChatColors.primaryBlue)
        }
        Spacer(Modifier.height(8.dp))
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = ChatColors.cardSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(ChatColors.primaryBlue),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(rdv.nomMedecin.take(2), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(rdv.nomMedecin, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = ChatColors.primaryNavy)
                        Text(rdv.specialite, fontSize = 12.sp, color = ChatColors.textSecondary)
                    }
                    if (rdv.estConfirme) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFEAF5FF))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            Text("CONFIRMÉ", fontSize = 10.sp, color = ChatColors.primaryBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = ChatColors.primaryBlue, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(rdv.dateTexte, fontSize = 12.sp, color = ChatColors.textSecondary)
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = ChatColors.primaryBlue, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(rdv.lieu, fontSize = 12.sp, color = ChatColors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { /* TODO: itinéraire */ },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ChatColors.primaryBlue),
                        contentPadding = PaddingValues(vertical = 8.dp),
                    ) {
                        Text("Itinéraire", fontSize = 13.sp)
                    }
                    OutlinedButton(
                        onClick = { /* TODO: détails */ },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(vertical = 8.dp),
                    ) {
                        Text("Détails", fontSize = 13.sp, color = ChatColors.primaryNavy)
                    }
                }
            }
        }
    }
}

@Composable
private fun MedicamentsSection(meds: List<MedicamentJour>, onValider: (String) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Médicaments du jour ${meds.count { !it.pris }}/${meds.size}",
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = ChatColors.primaryNavy,
            )
            Text("Gérer pilulier", fontSize = 12.sp, color = ChatColors.primaryBlue)
        }
        Spacer(Modifier.height(8.dp))
        meds.forEach { med ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = ChatColors.cardSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ChatColors.primaryBlue.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Medication, contentDescription = null, tint = ChatColors.primaryBlue, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(med.nom, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = ChatColors.primaryNavy)
                        Text(med.posologie, fontSize = 12.sp, color = ChatColors.textSecondary)
                    }
                    if (med.pris) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Pris", tint = ChatColors.validationSucces)
                    } else {
                        Button(
                            onClick = { onValider(med.nom) },
                            colors = ButtonDefaults.buttonColors(containerColor = ChatColors.validationSucces),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text("Valider la prise", fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

/** Barre de navigation basse partagée Home/Médecins/RDV/Messages/Profil. */
@Composable
fun HomeBottomBar(
    selected: String,
    onMessages: () -> Unit = {},
    onMedecins: () -> Unit = {},
    onRdv: () -> Unit = {},
) {
    val forme = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    val elements = listOf(
        Triple("Accueil", Icons.Default.Home, {}),
        Triple("Médecins", Icons.Default.People, onMedecins),
        Triple("RDV", Icons.Default.CalendarToday, onRdv),
        Triple("Messages", Icons.Default.ChatBubble, onMessages),
        Triple("Profil", Icons.Default.Person, {}),
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 12.dp, shape = forme, ambientColor = ChatColors.primaryNavy.copy(alpha = 0.12f)),
        shape = forme,
        color = Color(0xF0FFFFFF),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            elements.forEach { (label, icon, onClick) ->
                val actif = label == selected
                val couleur = if (actif) ChatColors.primaryBlue else ChatColors.textSecondary
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable(onClick = onClick),
                ) {
                    Icon(icon, contentDescription = label, tint = couleur, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.height(3.dp))
                    if (actif) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(ChatColors.primaryBlue))
                    } else {
                        Spacer(Modifier.size(6.dp))
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(label, fontSize = 11.sp, color = couleur, fontWeight = if (actif) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }
    }
}
