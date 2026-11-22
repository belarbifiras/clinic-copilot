package com.clinic.copilot.ui.chat

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clinic.copilot.domain.ContactFiche
import com.clinic.copilot.domain.Message
import com.clinic.copilot.domain.StatutMessage
import com.clinic.copilot.ui.contact.DoctorContactActivity
import com.clinic.copilot.ui.theme.ChatColors
import com.clinic.copilot.ui.theme.ChatFormatters
import com.clinic.copilot.ui.theme.ChatTypography

/**
 * Écran de discussion aligné sur les tokens du design system : bulles LazyColumn,
 * saisie, fiche contact par Intent explicite et bannière d'urgence par Intent implicite.
 *
 * @param onRetour Action de retour vers la liste des conversations.
 * @param viewModel ViewModel injecté par Hilt ; il lit l'identifiant et le nom du contact
 * depuis les arguments de navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onRetour: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var brouillon by rememberSaveable { mutableStateOf("") }
    val contexte = LocalContext.current

    LaunchedEffect(state.messages.lastOrNull()?.id) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }
    LaunchedEffect(state.lastSentMessageId) {
        if (state.lastSentMessageId != null && brouillon == state.lastSentContent) {
            brouillon = ""
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
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(ChatColors.primaryBlue)
                                .clickable {
                                    ouvrirFicheContact(
                                        contexte = contexte,
                                        titreContact = viewModel.titreContact,
                                        ficheContact = state.ficheContact,
                                    )
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = ChatFormatters.initiale(viewModel.titreContact),
                                style = ChatTypography.labelLg,
                                color = Color.White,
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = viewModel.titreContact,
                            style = ChatTypography.titleMd,
                            color = ChatColors.textMain,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onRetour) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour aux messages",
                            tint = ChatColors.textMain,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ChatColors.cardSurface,
                    titleContentColor = ChatColors.textMain,
                    navigationIconContentColor = ChatColors.textMain,
                ),
            )
        },
    ) { contentPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(contentPadding).imePadding()) {
            if (state.afficherBanniereUrgence) {
                BanniereUrgence(
                    surAppel = {
                        // Intent IMPLICITE : même pattern que DoctorContactActivity (ACTION_DIAL).
                        contexte.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:15")))
                    },
                    surMasquage = viewModel::masquerBanniereUrgence,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                TextButton(
                    onClick = {
                        ouvrirFicheContact(
                            contexte = contexte,
                            titreContact = viewModel.titreContact,
                            ficheContact = state.ficheContact,
                        )
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = ChatColors.primaryBlue),
                ) {
                    Text("Voir la fiche du contact", style = ChatTypography.bodySm)
                }

                state.errorMessage?.let { messageErreur ->
                    Column(
                        modifier = Modifier.padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            messageErreur,
                            style = ChatTypography.bodySm,
                            color = ChatColors.urgenceCritique,
                        )
                        Button(
                            onClick = viewModel::reessayer,
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

                if (state.hasOlderMessages) {
                    TextButton(
                        onClick = viewModel::chargerMessagesPrecedents,
                        enabled = !state.isLoadingOlder,
                        colors = ButtonDefaults.textButtonColors(contentColor = ChatColors.primaryBlue),
                    ) {
                        Text(
                            if (state.isLoadingOlder) "Chargement…" else "Charger les messages précédents",
                            style = ChatTypography.bodySm,
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when {
                        state.isLoading && state.messages.isEmpty() -> {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center),
                                color = ChatColors.primaryBlue,
                            )
                        }

                        state.messages.isEmpty() -> {
                            Text(
                                text = "Aucun message. Envoyez le premier.",
                                modifier = Modifier.align(Alignment.Center),
                                style = ChatTypography.bodyMd,
                                color = ChatColors.textSecondary,
                            )
                        }

                        else -> {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(vertical = 8.dp),
                            ) {
                                items(state.messages, key = { it.id }) { message ->
                                    MessageBubble(
                                        message = message,
                                        currentUserId = viewModel.utilisateurCourantId,
                                    )
                                }
                            }
                        }
                    }
                }

                state.sendErrorMessage?.let { erreur ->
                    Text(
                        text = erreur,
                        style = ChatTypography.bodySm,
                        color = ChatColors.urgenceCritique,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }

                val envoiActif = brouillon.isNotBlank() && !state.isSending
                val formeSaisie = RoundedCornerShape(24.dp)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .shadow(
                            elevation = 4.dp,
                            shape = formeSaisie,
                            ambientColor = ChatColors.primaryNavy.copy(alpha = 0.06f),
                            spotColor = ChatColors.primaryNavy.copy(alpha = 0.06f),
                        ),
                    shape = formeSaisie,
                    color = ChatColors.cardSurface,
                    border = BorderStroke(1.dp, Color(0x0A102D63)),
                    shadowElevation = 0.dp,
                    tonalElevation = 0.dp,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TextField(
                            value = brouillon,
                            onValueChange = { brouillon = it },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            placeholder = {
                                Text("Votre message", style = ChatTypography.bodyMd)
                            },
                            maxLines = 3,
                            textStyle = ChatTypography.bodyMd.copy(color = ChatColors.textMain),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                                focusedTextColor = ChatColors.textMain,
                                unfocusedTextColor = ChatColors.textMain,
                                cursorColor = ChatColors.primaryBlue,
                            ),
                        )
                        IconButton(
                            onClick = { viewModel.envoyerMessage(brouillon) },
                            enabled = envoiActif,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    color = if (envoiActif) {
                                        ChatColors.primaryBlue
                                    } else {
                                        ChatColors.textSecondary.copy(alpha = 0.20f)
                                    },
                                ),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Envoyer le message",
                                tint = if (envoiActif) Color.White else ChatColors.textSecondary,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Lance l'Intent EXPLICITE vers la fiche contact, depuis l'avatar de la barre supérieure
 * ou depuis le bouton « Voir la fiche du contact ».
 *
 * @param contexte Contexte Android utilisé pour démarrer l'activité cible.
 * @param titreContact Nom affiché dans la barre, utilisé comme valeur de repli.
 * @param ficheContact Fiche chargée par le ViewModel, éventuellement nulle.
 */
private fun ouvrirFicheContact(
    contexte: Context,
    titreContact: String,
    ficheContact: ContactFiche?,
) {
    val fiche = ficheContact ?: ContactFiche(
        nom = titreContact,
        specialite = "Contact",
        telephone = "01 00 00 00 00",
    )
    // Intent EXPLICITE : le composant cible et ses extras sont connus à la compilation.
    val intent = Intent(contexte, DoctorContactActivity::class.java).apply {
        putExtra(DoctorContactActivity.EXTRA_NOM, fiche.nom)
        putExtra(DoctorContactActivity.EXTRA_SPECIALITE, fiche.specialite)
        putExtra(DoctorContactActivity.EXTRA_TELEPHONE, fiche.telephone)
    }
    contexte.startActivity(intent)
}

/**
 * Bannière d'alerte médicale pleine largeur : fond #FDE8E8 et accent gauche 4dp.
 *
 * @param surAppel Déclenche l'appel immédiat vers le 15.
 * @param surMasquage Masque la bannière.
 */
@Composable
private fun BanniereUrgence(surAppel: () -> Unit, surMasquage: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(Color(0xFFFDE8E8)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(ChatColors.urgenceCritique),
        )
        Row(
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "En cas d'urgence immédiate, contactez le 15 ou le 112.",
                style = ChatTypography.bodySm,
                color = ChatColors.textMain,
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = surAppel,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ChatColors.urgenceCritique,
                    contentColor = Color.White,
                ),
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 4.dp),
                )
                Text("Appeler", style = ChatTypography.bodySm)
            }
            IconButton(onClick = surMasquage) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Masquer l'alerte",
                    tint = ChatColors.textSecondary,
                )
            }
        }
    }
}

/**
 * Bulle de message alignée à droite en primaryBlue pour mes messages,
 * à gauche en surface claire bordée pour les reçus.
 *
 * @param message Message à afficher.
 * @param currentUserId Identifiant de l'utilisateur courant.
 */
@Composable
private fun MessageBubble(message: Message, currentUserId: String) {
    val estMonMessage = message.expediteurId == currentUserId
    val forme = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (estMonMessage) Alignment.End else Alignment.Start,
    ) {
        Surface(
            shape = forme,
            color = if (estMonMessage) ChatColors.primaryBlue else ChatColors.medicalBackground,
            border = if (estMonMessage) null else BorderStroke(1.dp, Color(0x1F102D63)),
            shadowElevation = 0.dp,
            tonalElevation = 0.dp,
        ) {
            Text(
                text = message.contenu,
                style = ChatTypography.bodyMd,
                color = if (estMonMessage) Color.White else ChatColors.textMain,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
        val suffixe = if (estMonMessage && message.statut == StatutMessage.ECHEC) " · Échec" else ""
        Text(
            text = ChatFormatters.formaterHeure(message.dateEnvoi) + suffixe,
            style = ChatTypography.bodySm,
            color = ChatColors.textSecondary,
            modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp),
        )
    }
}
