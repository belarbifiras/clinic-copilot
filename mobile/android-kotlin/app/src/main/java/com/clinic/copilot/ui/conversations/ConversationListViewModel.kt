package com.clinic.copilot.ui.conversations

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clinic.copilot.data.ChatError
import com.clinic.copilot.data.ChatRepository
import com.clinic.copilot.data.local.ChatPreferences
import com.clinic.copilot.domain.Conversation
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Quatre états mutuellement exclusifs de l'écran de liste.
 */
enum class EtatListe {

    /** Premier chargement ou rafraîchissement en cours. */
    CHARGEMENT,

    /** Au moins une conversation est disponible. */
    DONNEES,

    /** Chargement terminé sans résultat. */
    VIDE,

    /** Le chargement a échoué, l'utilisateur peut réessayer. */
    ERREUR,
}

/**
 * Filtres rapides affichés dans la LazyRow de puces.
 *
 * @property libelle Libelle exact affiché sur la puce (le compteur « Non lus » est ajouté par l'écran).
 */
enum class FiltreConversation(val libelle: String) {

    /** Toutes les conversations. */
    TOUS("Tous"),

    /** Conversations avec un médecin identifié. */
    MEDECINS_TRAITANTS("Médecins traitants"),

    /** Conversations ayant au moins un message non lu. */
    NON_LUS("Non lus"),

    /** Conversations avec le secrétariat ou un cabinet. */
    SECRETARIAT("Secrétariat"),
}

/**
 * État affichable de la liste des conversations.
 *
 * @property etat État explicite pilotant le rendu (chargement / données / vide / erreur).
 * @property conversations Conversations triées du plus récent au plus ancien, avant filtrage.
 * @property conversationsFiltrees Résultat combinant la recherche et le filtre actif.
 * @property texteRecherche Texte saisi dans le champ de recherche.
 * @property filtreActif Puce de filtre sélectionnée.
 * @property errorMessage Message issu d'un [ChatError] ou d'une erreur inattendue.
 * @property notificationsActivees Valeur persistée lue depuis le DataStore.
 */
data class ConversationListUiState(
    val etat: EtatListe = EtatListe.CHARGEMENT,
    val conversations: List<Conversation> = emptyList(),
    val conversationsFiltrees: List<Conversation> = emptyList(),
    val texteRecherche: String = "",
    val filtreActif: FiltreConversation = FiltreConversation.TOUS,
    val errorMessage: String? = null,
    val notificationsActivees: Boolean = true,
) {

    /** Pastille d'alerte de la cloche : non lus uniquement si les notifications sont actives. */
    val notificationsEnAttente: Boolean
        get() = notificationsActivees && conversations.any { it.nombreNonLus > 0 }

    /** Nombre total de messages non lus, utilisé sur la puce « Non lus (n) ». */
    val totalNonLus: Int
        get() = conversations.sumOf { it.nombreNonLus }
}

/**
 * Charge les conversations, applique recherche et filtres, et exécute les actions du menu.
 *
 * @property repository Source de données sélectionnée par Hilt.
 * @property preferences Préférences DataStore de la messagerie.
 */
@HiltViewModel
class ConversationListViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val preferences: ChatPreferences,
) : ViewModel() {

    private val mutableUiState = MutableStateFlow(ConversationListUiState())
    private var observationJob: Job? = null

    /** État observable par l'écran Compose. */
    val uiState: StateFlow<ConversationListUiState> = mutableUiState.asStateFlow()

    init {
        charger()
        viewModelScope.launch {
            preferences.notificationsActivees.collect { actives ->
                mutableUiState.update { it.copy(notificationsActivees = actives) }
            }
        }
    }

    /**
     * (Re)charge la liste ; utilisé au démarrage et par l'option « Rafraîchir » du menu.
     */
    fun charger() {
        observationJob?.cancel()
        mutableUiState.update { it.copy(etat = EtatListe.CHARGEMENT, errorMessage = null) }
        observationJob = viewModelScope.launch {
            repository.observerConversations()
                .catch { cause ->
                    if (cause is CancellationException) throw cause
                    mutableUiState.update {
                        it.copy(
                            etat = EtatListe.ERREUR,
                            errorMessage = erreurLisible(cause),
                        )
                    }
                }
                .collect { conversations ->
                    val triees = conversations.sortedByDescending(Conversation::dateDernierMessage)
                    mutableUiState.update { etat ->
                        etat.copy(
                            etat = if (triees.isEmpty()) EtatListe.VIDE else EtatListe.DONNEES,
                            conversations = triees,
                            errorMessage = null,
                        ).avecListeFiltree()
                    }
                }
        }
    }

    /**
     * Met à jour la recherche ; le filtrage est recalculé immédiatement.
     *
     * @param texte Nouveau texte saisi.
     */
    fun mettreAJourRecherche(texte: String) {
        mutableUiState.update { it.copy(texteRecherche = texte).avecListeFiltree() }
    }

    /**
     * Change la puce de filtre active ; le filtrage est recalculé immédiatement.
     *
     * @param filtre Nouveau filtre sélectionné.
     */
    fun definirFiltre(filtre: FiltreConversation) {
        mutableUiState.update { it.copy(filtreActif = filtre).avecListeFiltree() }
    }

    /**
     * Marque toutes les conversations non lues comme lues, une par une via le dépôt.
     */
    fun toutMarquerCommeLu() {
        val aLire = mutableUiState.value.conversations.filter { it.nombreNonLus > 0 }
        if (aLire.isEmpty()) return

        viewModelScope.launch {
            try {
                aLire.forEach { repository.marquerCommeLu(it.id) }
                mutableUiState.update { etat ->
                    etat.copy(
                        conversations = etat.conversations.map { it.copy(nombreNonLus = 0) },
                    ).avecListeFiltree()
                }
            } catch (annulation: CancellationException) {
                throw annulation
            } catch (cause: Exception) {
                mutableUiState.update { it.copy(errorMessage = erreurLisible(cause)) }
            }
        }
    }

    /**
     * Inverse le réglage des notifications ; la valeur est persistée par le DataStore,
     * qui republie ensuite la nouvelle valeur dans l'état via son Flow.
     */
    fun basculerNotifications() {
        val newValue = !mutableUiState.value.notificationsActivees
        viewModelScope.launch {
            preferences.setNotificationsActivees(newValue)
        }
    }

    /**
     * Action du bouton secondaire « Nouveau message » de la carte Conseil IA.
     *
     * TODO: brancher sur le parcours complet de création de conversation (hors périmètre de cette feature).
     */
    fun nouveauMessage() {
        Log.i(TAG, "Nouveau message demandé depuis la carte Conseil IA")
        mutableUiState.update {
            it.copy(texteRecherche = "", filtreActif = FiltreConversation.TOUS)
                .avecListeFiltree()
        }
    }

    /**
     * Action du bouton intelligent « Triage IA » : le vrai traitement n'est pas dans cette feature.
     *
     * TODO: brancher sur le service de triage IA livré par une autre équipe (US-02).
     */
    fun triageIA() {
        Log.i(TAG, "Triage IA demandé depuis la carte Conseil IA (implémentation à venir)")
    }

    /** Convertit une exception en texte affichable. */
    private fun erreurLisible(cause: Throwable): String = when (cause) {
        is ChatError -> cause.messageUtilisateur
        else -> cause.message ?: "Impossible de charger les conversations."
    }

    /** Recalcule la liste résultante à partir de la liste source, de la recherche et du filtre. */
    private fun ConversationListUiState.avecListeFiltree(): ConversationListUiState =
        copy(conversationsFiltrees = conversations.filtrer(texteRecherche, filtreActif))

    /** Combine recherche textuelle et filtre rapide. */
    private fun List<Conversation>.filtrer(
        texte: String,
        filtre: FiltreConversation,
    ): List<Conversation> {
        val requete = texte.trim()
        return filter { conversation ->
            val correspondRecherche = requete.isBlank() ||
                conversation.nomContact.contains(requete, ignoreCase = true) ||
                conversation.dernierMessage.contains(requete, ignoreCase = true)

            val correspondFiltre = when (filtre) {
                FiltreConversation.TOUS -> true
                FiltreConversation.MEDECINS_TRAITANTS ->
                    conversation.nomContact.contains("dr", ignoreCase = true) ||
                        conversation.nomContact.contains("pr ", ignoreCase = true)
                FiltreConversation.NON_LUS -> conversation.nombreNonLus > 0
                FiltreConversation.SECRETARIAT ->
                    conversation.nomContact.contains("cabinet", ignoreCase = true) ||
                        conversation.nomContact.contains("secrét", ignoreCase = true) ||
                        conversation.nomContact.contains("secret", ignoreCase = true)
            }
            correspondRecherche && correspondFiltre
        }
    }

    private companion object {
        const val TAG = "ConversationListVM"
    }
}
