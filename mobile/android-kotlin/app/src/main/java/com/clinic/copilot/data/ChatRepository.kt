package com.clinic.copilot.data

import com.clinic.copilot.domain.ContactFiche
import com.clinic.copilot.domain.Conversation
import com.clinic.copilot.domain.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Contrat d'accès aux conversations et aux messages, indépendant de la source de données.
 *
 * L'implémentation utilisée est choisie dans [com.clinic.copilot.di.ChatModule].
 */
interface ChatRepository {

    /**
     * Observe la liste des conversations de l'utilisateur courant.
     *
     * @return Un flux de conversations, triées par ordre chronologique décroissant.
     */
    fun observerConversations(): Flow<List<Conversation>>

    /**
     * Charge une page d'historique, du message le plus ancien au plus récent.
     *
     * @param conversationId Identifiant de la conversation.
     * @param page Numéro de page basé sur 1.
     * @param limit Nombre maximal de messages.
     * @return Les messages de la page demandée.
     */
    suspend fun chargerHistorique(
        conversationId: String,
        page: Int = DEFAULT_PAGE,
        limit: Int = DEFAULT_PAGE_SIZE,
    ): List<Message>

    /**
     * Envoie un nouveau message.
     *
     * @param conversationId Identifiant de la conversation.
     * @param expediteurId Identifiant de l'expéditeur.
     * @param contenu Texte du message.
     * @return Le message accepté par la source de données.
     */
    suspend fun envoyerMessage(
        conversationId: String,
        expediteurId: String,
        contenu: String,
    ): Message

    /**
     * Marque comme lus les messages reçus d'une conversation.
     *
     * @param conversationId Identifiant de la conversation.
     */
    suspend fun marquerCommeLu(conversationId: String)

    /**
     * Fournit les coordonnées minimales du contact d'une conversation.
     *
     * @param conversationId Identifiant de la conversation.
     * @return Une fiche par défaut si la source de données n'en expose pas.
     */
    suspend fun obtenirFicheContact(conversationId: String): ContactFiche = ContactFiche(
        nom = "Contact",
        specialite = "Praticien",
        telephone = "01 00 00 00 00",
    )

    /**
     * Extension optionnelle pour le futur flux temps réel (Socket.IO).
     *
     * @param conversationId Identifiant de la conversation à observer.
     * @return Un flux vide par défaut ; seul le dépôt factice émet aujourd'hui des messages.
     */
    fun observerMessagesTempsReel(conversationId: String): Flow<Message> = emptyFlow()

    /** Valeurs par défaut partagées avec les ViewModels. */
    companion object {
        /** Première page de l'historique. */
        const val DEFAULT_PAGE = 1

        /** Taille de page par défaut, identique à celle du chat-service. */
        const val DEFAULT_PAGE_SIZE = 30
    }
}
