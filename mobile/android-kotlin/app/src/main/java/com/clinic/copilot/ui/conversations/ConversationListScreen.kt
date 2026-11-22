package com.clinic.copilot.ui.conversations

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clinic.copilot.R
import com.clinic.copilot.domain.Conversation
import com.clinic.copilot.ui.theme.ChatColors
import com.clinic.copilot.ui.theme.ChatFormatters
import com.clinic.copilot.ui.theme.ChatTypography

/**
 * Écran de messagerie aligné sur le design system officiel : fond medicalBackground,
 * en-tête mascotte, bandeau de confidentialité, recherche, puces de filtre, cartes
 * de praticien niveau 1, carte Conseil IA niveau 3 et navigation basse niveau 4.
 *
 * Le menu 3 points historique (marquer tout comme lu / notifications / rafraîchir)
 * reste fonctionnel et est conservé dans la barre supérieure.
 *
 * @param onConversationClick Callback invoqué avec la conversation choisie (identifiant et nom).
 * @param viewModel ViewModel injecté par Hilt, remplaçable dans les tests Compose.
 */
// TODO: ajouter la navigation retour vers l'accueil global une fois la navigation par rôle de l'équipe (US-05) branchée
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationListScreen(
    onConversationClick: (Conversation) -> Unit,
    viewModel: ConversationListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var menuOuvert by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = ChatColors.medicalBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.ic_copilot_mascot),
                            contentDescription = "Logo Clinic Copilot",
                            modifier = Modifier.size(32.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop,
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Clinic Copilot",
                                style = ChatTypography.titleMd,
                                color = ChatColors.textMain,
                            )
                            Text(
                                text = "Accueil",
                                style = ChatTypography.bodySm,
                                color = ChatColors.textSecondary,
                            )
                        }
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { /* TODO: centre de notifications (autre feature) */ }) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = ChatColors.textSecondary,
                            )
                        }
                        if (state.notificationsEnAttente) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 8.dp, end = 8.dp)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(ChatColors.urgenceCritique),
                            )
                        }
                    }
                    IconButton(onClick = { menuOuvert = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Ouvrir le menu",
                            tint = ChatColors.textMain,
                        )
                    }
                    DropdownMenu(
                        expanded = menuOuvert,
                        onDismissRequest = { menuOuvert = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Tout marquer comme lu", style = ChatTypography.bodyMd) },
                            onClick = {
                                menuOuvert = false
                                viewModel.toutMarquerCommeLu()
                            },
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (state.notificationsActivees) {
                                        "Désactiver les notifications"
                                    } else {
                                        "Activer les notifications"
                                    },
                                    style = ChatTypography.bodyMd,
                                )
                            },
                            onClick = {
                                menuOuvert = false
                                viewModel.basculerNotifications()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Rafraîchir", style = ChatTypography.bodyMd) },
                            onClick = {
                                menuOuvert = false
                                viewModel.charger()
                            },
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ChatColors.cardSurface,
                    titleContentColor = ChatColors.textMain,
                    actionIconContentColor = ChatColors.textMain,
                ),
            )
        },
        bottomBar = { BottomNavigationBar() },
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { CarteConfidentialite() }
            item {
                ChampRecherche(
                    valeur = state.texteRecherche,
                    surChangement = viewModel::mettreAJourRecherche,
                )
            }
            item {
                PucesFiltre(
                    filtreActif = state.filtreActif,
                    totalNonLus = state.totalNonLus,
                    surFiltre = viewModel::definirFiltre,
                )
            }
            item {
                CarteConseilIA(
                    nouveauMessage = viewModel::nouveauMessage,
                    triageIA = viewModel::triageIA,
                )
            }

            when (state.etat) {
                EtatListe.CHARGEMENT -> {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = ChatColors.primaryBlue)
                        }
                    }
                }

                EtatListe.ERREUR -> {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = state.errorMessage ?: "Impossible de charger les conversations.",
                                style = ChatTypography.bodyMd,
                                color = ChatColors.urgenceCritique,
                            )
                            Button(
                                onClick = viewModel::charger,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ChatColors.primaryBlue,
                                    contentColor = Color.White,
                                ),
                            ) {
                                Text("Réessayer", style = ChatTypography.labelLg)
                            }
                        }
                    }
                }

                EtatListe.VIDE, EtatListe.DONNEES -> {
                    if (state.conversationsFiltrees.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChatBubbleOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = ChatColors.textSecondary,
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = "Aucune conversation pour l'instant.",
                                    style = ChatTypography.bodyMd,
                                    color = ChatColors.textSecondary,
                                )
                            }
                        }
                    } else {
                        items(state.conversationsFiltrees, key = { it.id }) { conversation ->
                            CarteConversation(
                                conversation = conversation,
                                onClick = { onConversationClick(conversation) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Bandeau de confidentialité en surface lightBlueSurface, coins arrondis 16dp. */
@Composable
private fun CarteConfidentialite() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = ChatColors.lightBlueSurface,
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = ChatColors.primaryBlue,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = "Vos données sont protégées",
                    style = ChatTypography.titleMd,
                    color = ChatColors.textMain,
                )
                Text(
                    text = "Vos échanges avec votre équipe soignante restent confidentiels et chiffrés.",
                    style = ChatTypography.bodySm,
                    color = ChatColors.textSecondary,
                )
            }
        }
    }
}

/**
 * Champ de recherche 52dp : fond blanc, rayon 12dp, bordure 12 % de primaryNavy,
 * bordure 2px primaryBlue et glow diffus au focus.
 *
 * @param valeur Texte courant, possédé par le ViewModel.
 * @param surChangement Callback de mise à jour du texte.
 */
@Composable
private fun ChampRecherche(valeur: String, surChangement: (String) -> Unit) {
    var aLeFocus by remember { mutableStateOf(false) }
    val contexte = LocalContext.current

    val lanceurVocal = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { resultat ->
        if (resultat.resultCode == Activity.RESULT_OK) {
            val texteReconnu = resultat.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.trim()
                .orEmpty()
            if (texteReconnu.isNotEmpty()) {
                surChangement(texteReconnu)
            }
        }
    }

    val lanceurPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { accordee ->
        if (accordee) {
            lancerReconnaissanceVocale(contexte, lanceurVocal)
        } else {
            Toast.makeText(
                contexte,
                "Autorisez le microphone pour utiliser la recherche vocale.",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    val forme = RoundedCornerShape(12.dp)
    val couleurBordure = if (aLeFocus) ChatColors.primaryBlue else Color(0x1F102D63)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(
                elevation = if (aLeFocus) 8.dp else 2.dp,
                shape = forme,
                ambientColor = ChatColors.primaryBlue.copy(alpha = 0.25f),
                spotColor = ChatColors.primaryBlue.copy(alpha = 0.25f),
            )
            .background(ChatColors.cardSurface, forme)
            .border(if (aLeFocus) 2.dp else 1.dp, couleurBordure, forme)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Rechercher",
            tint = ChatColors.textSecondary,
            modifier = Modifier.size(20.dp),
        )
        BasicTextField(
            value = valeur,
            onValueChange = surChangement,
            singleLine = true,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp)
                .onFocusChanged { aLeFocus = it.isFocused },
            textStyle = ChatTypography.bodyMd.copy(color = ChatColors.textMain),
            decorationBox = { champInterne ->
                if (valeur.isEmpty()) {
                    Text(
                        text = "Rechercher un contact ou un message",
                        style = ChatTypography.bodyMd,
                        color = ChatColors.textSecondary,
                    )
                }
                champInterne()
            },
        )
        IconButton(
            onClick = {
                if (contexte.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    lancerReconnaissanceVocale(contexte, lanceurVocal)
                } else {
                    lanceurPermission.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Saisie vocale",
                tint = ChatColors.textSecondary,
            )
        }
    }
}

/**
 * Démarre la reconnaissance vocale native en français et transmet le résultat au lanceur.
 *
 * @param contexte Contexte Android utilisé pour afficher un message d'erreur éventuel.
 * @param lanceur Lanceur d'activité qui reçoit le résultat de la reconnaissance.
 */
private fun lancerReconnaissanceVocale(
    contexte: Context,
    lanceur: ActivityResultLauncher<Intent>,
) {
    val intention = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
        )
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR")
        putExtra(RecognizerIntent.EXTRA_PROMPT, "Dites le nom d'un contact ou un message")
    }
    try {
        lanceur.launch(intention)
    } catch (activiteIntrouvable: ActivityNotFoundException) {
        Toast.makeText(
            contexte,
            "Aucune application de reconnaissance vocale n'est disponible.",
            Toast.LENGTH_SHORT,
        ).show()
    }
}

/**
 * Puces de filtre 36dp, rounded-full : actif en primaryBlue avec ombre bleue,
 * inactif en blanc bordé #E2E8F0.
 *
 * @param filtreActif Puce sélectionnée.
 * @param totalNonLus Compteur affiché sur la puce « Non lus ».
 * @param surFiltre Callback de changement de filtre.
 */
@Composable
private fun PucesFiltre(
    filtreActif: FiltreConversation,
    totalNonLus: Int,
    surFiltre: (FiltreConversation) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(FiltreConversation.entries, key = { it.name }) { filtre ->
            val actif = filtre == filtreActif
            val libelle = if (filtre == FiltreConversation.NON_LUS) {
                "${filtre.libelle} ($totalNonLus)"
            } else {
                filtre.libelle
            }
            val forme = RoundedCornerShape(50)

            Surface(
                modifier = Modifier
                    .height(36.dp)
                    .clip(forme)
                    .clickable { surFiltre(filtre) }
                    .then(
                        if (actif) {
                            Modifier.shadow(
                                elevation = 6.dp,
                                shape = forme,
                                ambientColor = ChatColors.primaryBlue.copy(alpha = 0.35f),
                                spotColor = ChatColors.primaryBlue.copy(alpha = 0.35f),
                            )
                        } else {
                            Modifier
                        },
                    ),
                shape = forme,
                color = if (actif) ChatColors.primaryBlue else ChatColors.cardSurface,
                border = if (actif) null else BorderStroke(1.dp, Color(0xFFE2E8F0)),
            ) {
                Box(Modifier.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = libelle,
                        style = ChatTypography.bodySm.copy(
                            color = if (actif) Color.White else ChatColors.textMain,
                            fontWeight = if (actif) FontWeight.SemiBold else FontWeight.Normal,
                        ),
                    )
                }
            }
        }
    }
}

/**
 * Carte Conseil IA de niveau 3 : bordure dégradée turquoise → bleu et glow turquoise.
 *
 * @param nouveauMessage Action du bouton secondaire.
 * @param triageIA Action du bouton intelligent (TODO côté ViewModel).
 */
@Composable
private fun CarteConseilIA(nouveauMessage: () -> Unit, triageIA: () -> Unit) {
    val forme = RoundedCornerShape(16.dp)
    val bordure = Brush.linearGradient(
        listOf(
            ChatColors.turquoiseAccent.copy(alpha = 0.4f),
            ChatColors.primaryBlue.copy(alpha = 0.1f),
        ),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = forme,
                ambientColor = ChatColors.turquoiseAccent.copy(alpha = 0.35f),
                spotColor = ChatColors.turquoiseAccent.copy(alpha = 0.25f),
            )
            .background(ChatColors.cardSurface, forme)
            .border(1.dp, bordure, forme)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.ic_copilot_mascot),
                contentDescription = "Assistant IA Copilot",
                modifier = Modifier.size(56.dp).clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Conseil IA",
                    style = ChatTypography.titleMd,
                    color = ChatColors.textMain,
                )
                Text(
                    text = "Copilot peut vous aider à prioriser vos messages et préparer vos réponses.",
                    style = ChatTypography.bodySm,
                    color = ChatColors.textSecondary,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = nouveauMessage,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ChatColors.lightBlueSurface,
                    contentColor = ChatColors.primaryBlue,
                ),
            ) {
                Text("Nouveau message", style = ChatTypography.bodySm)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(8.dp),
                        ambientColor = ChatColors.turquoiseAccent.copy(alpha = 0.4f),
                        spotColor = ChatColors.turquoiseAccent.copy(alpha = 0.4f),
                    )
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(ChatColors.primaryBlue, ChatColors.turquoiseAccent),
                        ),
                    )
                    .clickable(onClick = triageIA),
                contentAlignment = Alignment.Center,
            ) {
                Text("Triage IA", style = ChatTypography.bodySm, color = Color.White)
            }
        }
    }
}

/**
 * Fiche de praticien adaptée à la messagerie : carte blanche niveau 1
 * (ombre douce et bordure 4 % de primaryNavy), avatar 48dp et badge conditionnel.
 *
 * @param conversation Conversation à afficher.
 * @param onClick Action déclenchée au clic sur la carte.
 */
@Composable
private fun CarteConversation(conversation: Conversation, onClick: () -> Unit) {
    val forme = RoundedCornerShape(16.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = forme,
                ambientColor = ChatColors.primaryNavy.copy(alpha = 0.06f),
                spotColor = ChatColors.primaryNavy.copy(alpha = 0.06f),
            )
            .clickable(onClick = onClick),
        shape = forme,
        color = ChatColors.cardSurface,
        border = BorderStroke(1.dp, Color(0x0A102D63)),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(ChatColors.primaryBlue),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = ChatFormatters.initiale(conversation.nomContact),
                    style = ChatTypography.labelLg,
                    color = Color.White,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = conversation.nomContact,
                    style = ChatTypography.titleMd,
                    color = ChatColors.textMain,
                )
                Text(
                    text = ChatFormatters.categorieContact(conversation.nomContact),
                    style = ChatTypography.bodySm,
                    color = ChatColors.textSecondary,
                )
                Text(
                    text = conversation.dernierMessage.ifBlank { "Nouvelle conversation" },
                    style = ChatTypography.bodySm,
                    color = ChatColors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = ChatFormatters.formaterHeure(conversation.dateDernierMessage),
                    style = ChatTypography.bodySm,
                    color = ChatColors.textSecondary,
                )
                if (conversation.nombreNonLus > 0) {
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(ChatColors.urgenceCritique),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = conversation.nombreNonLus.toString(),
                            style = ChatTypography.labelSm,
                            color = Color.White,
                        )
                    }
                }
            }
        }
    }
}

// TODO: provisoire - à rebrancher sur la vraie navigation par rôle de l'équipe (US-05) une fois livrée, ceci est local à ma feature uniquement
/**
 * Barre d'onglets locale de niveau 4 : fond blanc 94 %, coins hauts arrondis 16dp
 * et ombre vers le haut. L'élément « Messages » est l'onglet actif.
 */
@Composable
private fun BottomNavigationBar() {
    val forme = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    val elements = listOf(
        ElementNavigation("Accueil", Icons.Default.Home),
        ElementNavigation("Médecins", Icons.Default.People),
        ElementNavigation("RDV", Icons.Default.CalendarToday),
        ElementNavigation("Messages", Icons.Default.ChatBubble),
        ElementNavigation("Profil", Icons.Default.Person),
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = forme,
                ambientColor = ChatColors.primaryNavy.copy(alpha = 0.12f),
                spotColor = ChatColors.primaryNavy.copy(alpha = 0.12f),
            ),
        shape = forme,
        // Fallback : translucidité 94 % ; le backdrop-blur 16dp n'est pas disponible nativement en Compose.
        color = Color(0xF0FFFFFF),
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            elements.forEach { element ->
                val actif = element.libelle == "Messages"
                val couleur = if (actif) ChatColors.primaryBlue else ChatColors.textSecondary
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { /* TODO: navigation par rôle (US-05) */ },
                ) {
                    Icon(
                        imageVector = element.icone,
                        contentDescription = element.libelle,
                        tint = couleur,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.height(3.dp))
                    if (actif) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(ChatColors.primaryBlue),
                        )
                    } else {
                        Spacer(Modifier.size(6.dp))
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = element.libelle,
                        style = ChatTypography.bodySm.copy(
                            color = couleur,
                            fontWeight = if (actif) FontWeight.SemiBold else FontWeight.Normal,
                        ),
                    )
                }
            }
        }
    }
}

/** Entrée d'un onglet de la navigation basse. */
private data class ElementNavigation(val libelle: String, val icone: ImageVector)
