package com.clinic.copilot.ui.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clinic.copilot.data.ChatError
import com.clinic.copilot.data.ChatRepository
import com.clinic.copilot.data.local.ChatPreferences
import com.clinic.copilot.domain.ContactFiche
import com.clinic.copilot.domain.Message
import com.clinic.copilot.domain.StatutMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * État affichable de l'écran d'une conversation.
 *
 * @property isLoading Chargement de la première page en cours.
 * @property isLoadingOlder Chargement d'une page plus ancienne en cours.
 * @property isSending Envoi du message en cours.
 * @property hasOlderMessages Une page précédente est disponible.
 * @property messages Messages affichés, du plus ancien au plus récent.
 * @property errorMessage Erreur de chargement, affichée avec un bouton « Réessayer ».
 * @property sendErrorMessage Erreur d'envoi, distincte de l'erreur de chargement.
 * @property ficheContact Coordonnées utilisées par l'Intent explicite vers la fiche.
 * @property afficherBanniereUrgence Vrai lorsqu'un message urgent vient d'arriver dans le chat.
 * @property lastSentMessageId Identifiant du dernier message envoyé, pour vider la saisie.
 * @property lastSentContent Contenu du dernier message envoyé, comparé au brouillon.
 */
data class ChatUiState(
    val isLoading: Boolean = true,
    val isLoadingOlder: Boolean = false,
    val isSending: Boolean = false,
    val hasOlderMessages: Boolean = false,
    val messages: List<Message> = emptyList(),
    val errorMessage: String? = null,
    val sendErrorMessage: String? = null,
    val ficheContact: ContactFiche? = null,
    val afficherBanniereUrgence: Boolean = false,
    val lastSentMessageId: String? = null,
    val lastSentContent: String? = null,
)

/**
 * Gère l'historique paginé, l'envoi, la lecture et la persistance de la conversation ouverte.
 *
 * @property repository Source de données sélectionnée par Hilt.
 * @property preferences DataStore utilisé pour mémoriser la dernière conversation ouverte.
 * @property savedStateHandle Paramètres transmis par le graphe de navigation local.
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val preferences: ChatPreferences,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val conversationId: String = savedStateHandle.get<String>("conversationId").orEmpty()
    private val contactDepuisNavigation: String = savedStateHandle.get<String>("nomContact").orEmpty()
    private val currentUserId: String = savedStateHandle.get<String>("currentUserId")
        ?.takeIf { it.isNotBlank() }
        ?: "patient-demo"

    private val mutableUiState = MutableStateFlow(ChatUiState())
    private var prochainePage = 2

    /** Titre de l'écran, issu du paramètre réellement transmis par la navigation. */
    val titreContact: String = contactDepuisNavigation.ifBlank { "Conversation" }

    /** Identifiant de session utilisé par l'écran pour aligner les bulles. */
    val utilisateurCourantId: String = currentUserId

    /** État observable par l'écran Compose. */
    val uiState: StateFlow<ChatUiState> = mutableUiState.asStateFlow()

    init {
        if (conversationId.isBlank()) {
            mutableUiState.update {
                it.copy(isLoading = false, errorMessage = "Identifiant de conversation manquant.")
            }
        } else {
            viewModelScope.launch { preferences.enregistrerDerniereConversation(conversationId) }
            chargerPremierePage()
            chargerFicheContact()
            observerTempsReel()
            marquerCommeLu()
        }
    }

    /**
     * Masque la bannière d'urgence après prise de connaissance par l'utilisateur.
     */
    fun masquerBanniereUrgence() {
        mutableUiState.update { it.copy(afficherBanniereUrgence = false) }
    }

    /**
     * Recharge la première page après une erreur.
     */
    fun reessayer() {
        if (conversationId.isBlank()) return
        mutableUiState.update { it.copy(isLoading = true, errorMessage = null) }
        chargerPremierePage()
    }

    /**
     * Charge la page d'historique précédente.
     */
    fun chargerMessagesPrecedents() {
        val etat = mutableUiState.value
        if (etat.isLoadingOlder || !etat.hasOlderMessages || conversationId.isBlank()) return

        mutableUiState.update { it.copy(isLoadingOlder = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val page = repository.chargerHistorique(
                    conversationId = conversationId,
                    page = prochainePage,
                    limit = PAGE_SIZE,
                )
                prochainePage += 1
                mutableUiState.update { state ->
                    state.copy(
                        isLoadingOlder = false,
                        hasOlderMessages = page.size == PAGE_SIZE,
                        messages = (page + state.messages)
                            .distinctBy(Message::id)
                            .sortedBy(Message::dateEnvoi),
                    )
                }
            } catch (annulation: CancellationException) {
                throw annulation
            } catch (cause: Exception) {
                mutableUiState.update {
                    it.copy(isLoadingOlder = false, errorMessage = erreurLisible(cause))
                }
            }
        }
    }

    /**
     * Envoie le texte saisi ; l'expéditeur est fourni par la session détenue par le ViewModel.
     *
     * @param contenu Texte saisi dans la barre de message.
     */
    fun envoyerMessage(contenu: String) {
        val texte = contenu.trim()
        if (texte.isBlank()) {
            mutableUiState.update { it.copy(sendErrorMessage = "Le message ne peut pas être vide.") }
            return
        }
        if (mutableUiState.value.isSending || conversationId.isBlank()) return

        mutableUiState.update { it.copy(isSending = true, sendErrorMessage = null) }
        viewModelScope.launch {
            try {
                val message = repository.envoyerMessage(
                    conversationId = conversationId,
                    expediteurId = currentUserId,
                    contenu = texte,
                )
                ajouterMessage(message)
                mutableUiState.update {
                    it.copy(
                        isSending = false,
                        sendErrorMessage = null,
                        lastSentMessageId = message.id,
                        lastSentContent = texte,
                    )
                }
            } catch (annulation: CancellationException) {
                throw annulation
            } catch (cause: Exception) {
                // On conserve le texte dans la conversation avec le statut ÉCHEC : l'échec est visible.
                ajouterMessage(
                    Message(
                        id = "echec-${System.currentTimeMillis()}",
                        conversationId = conversationId,
                        expediteurId = currentUserId,
                        contenu = texte,
                        dateEnvoi = System.currentTimeMillis(),
                        statut = StatutMessage.ECHEC,
                    ),
                )
                mutableUiState.update {
                    it.copy(isSending = false, sendErrorMessage = erreurLisible(cause))
                }
            }
        }
    }

    private fun chargerPremierePage() {
        viewModelScope.launch {
            try {
                val page = repository.chargerHistorique(
                    conversationId = conversationId,
                    page = ChatRepository.DEFAULT_PAGE,
                    limit = PAGE_SIZE,
                )
                mutableUiState.update {
                    it.copy(
                        isLoading = false,
                        hasOlderMessages = page.size == PAGE_SIZE,
                        messages = page.sortedBy(Message::dateEnvoi),
                        errorMessage = null,
                    )
                }
            } catch (annulation: CancellationException) {
                throw annulation
            } catch (cause: Exception) {
                mutableUiState.update {
                    it.copy(isLoading = false, errorMessage = erreurLisible(cause))
                }
            }
        }
    }

    private fun chargerFicheContact() {
        viewModelScope.launch {
            try {
                val fiche = repository.obtenirFicheContact(conversationId)
                mutableUiState.update { it.copy(ficheContact = fiche) }
            } catch (cause: Exception) {
                // La fiche reste facultative : on affiche au moins le nom reçu par la navigation.
                mutableUiState.update {
                    it.copy(
                        ficheContact = ContactFiche(
                            nom = titreContact,
                            specialite = "Contact",
                            telephone = "01 00 00 00 00",
                        ),
                    )
                }
            }
        }
    }

    private fun observerTempsReel() {
        viewModelScope.launch {
            repository.observerMessagesTempsReel(conversationId)
                .catch { cause ->
                    if (cause is CancellationException) throw cause
                    mutableUiState.update { it.copy(errorMessage = erreurLisible(cause)) }
                }
                .collect(::traiterMessageEntrant)
        }
    }

    /**
     * Traite un message reçu en temps réel et lève la bannière d'urgence si son contenu le demande.
     *
     * @param message Message entrant.
     */
    private fun traiterMessageEntrant(message: Message) {
        ajouterMessage(message)
        if (message.expediteurId != currentUserId && estMessageUrgent(message.contenu)) {
            mutableUiState.update { it.copy(afficherBanniereUrgence = true) }
        }
    }

    /** Détecte les mots-clés d'urgence dans un message reçu. */
    private fun estMessageUrgent(contenu: String): Boolean {
        val texte = contenu.lowercase(Locale.getDefault())
        return MOTS_URGENCE.any { texte.contains(it) }
    }

    private fun marquerCommeLu() {
        viewModelScope.launch {
            try {
                repository.marquerCommeLu(conversationId)
            } catch (annulation: CancellationException) {
                throw annulation
            } catch (cause: Exception) {
                mutableUiState.update { it.copy(errorMessage = erreurLisible(cause)) }
            }
        }
    }

    private fun ajouterMessage(message: Message) {
        mutableUiState.update { etat ->
            etat.copy(
                messages = (etat.messages + message)
                    .distinctBy(Message::id)
                    .sortedBy(Message::dateEnvoi),
            )
        }
    }

    /** Convertit une exception en texte affichable. */
    private fun erreurLisible(cause: Throwable): String = when (cause) {
        is ChatError -> cause.messageUtilisateur
        else -> cause.message ?: "Une erreur est survenue."
    }

    private companion object {
        const val PAGE_SIZE = 30
        val MOTS_URGENCE = listOf("urgence", "urgent", "critique", "immédiat", "immediat")
    }
}
