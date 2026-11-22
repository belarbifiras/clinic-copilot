package com.clinic.copilot.data

import com.clinic.copilot.core.session.TokenProvider
import com.clinic.copilot.data.remote.ChatApiService
import com.clinic.copilot.data.remote.dto.ConversationDto
import com.clinic.copilot.data.remote.dto.EnvoyerMessageRequest
import com.clinic.copilot.data.remote.dto.MessageDto
import com.clinic.copilot.domain.ContactFiche
import com.clinic.copilot.domain.Conversation
import com.clinic.copilot.domain.Message
import com.clinic.copilot.domain.StatutMessage
import com.squareup.moshi.Moshi
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Implémentation réelle du dépôt, branchée sur le chat-service via [ChatApiService].
 *
 * Toutes les erreurs HTTP sont converties en [ChatError] avant d'atteindre les ViewModels.
 */
@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val api: ChatApiService,
    private val moshi: Moshi,
    private val tokenProvider: TokenProvider,
) : ChatRepository {

    /**
     * Observe la liste fournie par `GET /conversations`.
     *
     * @return Un flux contenant la liste triée du dernier message au plus ancien.
     */
    override fun observerConversations(): Flow<List<Conversation>> = flow {
        val conversations = appeler { api.obtenirConversations() }
        val monIdentifiant = tokenProvider.obtenirUtilisateurId()
        emit(
            conversations
                .map { it.versDomaine(monIdentifiant) }
                .sortedByDescending(Conversation::dateDernierMessage),
        )
    }

    /**
     * Charge une page de `GET /conversations/{id}/messages`.
     *
     * @param conversationId Identifiant de la conversation.
     * @param page Numéro de page basé sur 1.
     * @param limit Taille de la page.
     * @return Les messages dans l'ordre chronologique.
     */
    override suspend fun chargerHistorique(
        conversationId: String,
        page: Int,
        limit: Int,
    ): List<Message> {
        val monIdentifiant = tokenProvider.obtenirUtilisateurId()
        return appeler { api.obtenirMessages(conversationId, page, limit) }
            .map { it.versDomaine(conversationId, monIdentifiant) }
            .sortedBy(Message::dateEnvoi)
    }

    /**
     * Envoie un message via `POST /conversations/{id}/messages`.
     *
     * L'expéditeur n'est pas envoyé : le serveur le déduit du JWT (source de sécurité).
     *
     * @param conversationId Identifiant de la conversation.
     * @param expediteurId Identifiant local, utilisé uniquement si la réponse est incomplète.
     * @param contenu Texte du message.
     * @return Le message persisté par le serveur.
     */
    override suspend fun envoyerMessage(
        conversationId: String,
        expediteurId: String,
        contenu: String,
    ): Message {
        val texte = contenu.trim()
        val monIdentifiant = tokenProvider.obtenirUtilisateurId()
        return appeler {
            api.envoyerMessage(conversationId, EnvoyerMessageRequest(content = texte))
        }.versDomaine(conversationId, monIdentifiant, expediteurId)
    }

    /**
     * Appelle `PATCH /conversations/{id}/read`.
     *
     * @param conversationId Identifiant de la conversation.
     */
    override suspend fun marquerCommeLu(conversationId: String) {
        appeler { api.marquerCommeLu(conversationId) }
    }

    /**
     * Déduit la fiche contact de la conversation renvoyée par l'API.
     *
     * @param conversationId Identifiant de la conversation.
     * @return Nom du contact, rôle et numéro fictif pour la démonstration d'Intent.
     */
    override suspend fun obtenirFicheContact(conversationId: String): ContactFiche {
        val monIdentifiant = tokenProvider.obtenirUtilisateurId()
        val conversation = appeler { api.obtenirConversations() }
            .firstOrNull { it.id == conversationId }

        if (conversation == null) {
            return super.obtenirFicheContact(conversationId)
        }
        val contactId = conversation.contactId(monIdentifiant)
        return ContactFiche(
            nom = conversation.nomContact(monIdentifiant),
            specialite = if (contactId?.startsWith("doctor", ignoreCase = true) == true) {
                "Médecin"
            } else {
                "Secrétariat / Cabinet"
            },
            telephone = "01 00 00 00 00",
        )
    }

    /** Exécute un appel réseau et convertit ses erreurs en [ChatError]. */
    private suspend fun <T> appeler(bloc: suspend () -> T): T = try {
        bloc()
    } catch (annulation: CancellationException) {
        throw annulation
    } catch (cause: Exception) {
        throw cause.commeChatError(moshi)
    }

    /** Identifiant du participant qui n'est pas l'utilisateur courant. */
    private fun ConversationDto.contactId(monIdentifiant: String): String? = when {
        patientId == monIdentifiant -> doctorId
        doctorId == monIdentifiant -> patientId
        else -> doctorId ?: patientId
    }

    /** Nom du contact : champ enrichi de la gateway, sinon dérivé de l'identifiant. */
    private fun ConversationDto.nomContact(monIdentifiant: String): String {
        val contactId = contactId(monIdentifiant)
        return contactName?.takeIf { it.isNotBlank() } ?: when {
            contactId.isNullOrBlank() -> "Cabinet médical"
            contactId.startsWith("doctor", ignoreCase = true) -> {
                val suffixe = contactId.substringAfter("doctor").trim('-', '_')
                "Dr. ${suffixe.replace('-', ' ').replaceFirstChar { it.uppercase() }}"
            }
            contactId.startsWith("clinic", ignoreCase = true) -> "Cabinet médical"
            else -> contactId
        }
    }

    /** Convertit le DTO JSON en modèle métier. */
    private fun ConversationDto.versDomaine(monIdentifiant: String) = Conversation(
        id = id,
        nomContact = nomContact(monIdentifiant),
        dernierMessage = lastMessage.orEmpty(),
        dateDernierMessage = parseDate(lastMessageAt).takeIf { it != 0L } ?: parseDate(updatedAt),
        nombreNonLus = unreadCount?.coerceAtLeast(0) ?: 0,
    )

    /** Convertit un message JSON en modèle métier. */
    private fun MessageDto.versDomaine(
        conversationId: String,
        monIdentifiant: String,
        fallbackExpediteur: String = monIdentifiant,
    ) = Message(
        id = id.ifBlank { UUID.randomUUID().toString() },
        conversationId = this@versDomaine.conversationId?.takeIf { it.isNotBlank() } ?: conversationId,
        expediteurId = senderId?.takeIf { it.isNotBlank() } ?: fallbackExpediteur,
        contenu = content.orEmpty(),
        dateEnvoi = parseDate(createdAt),
        statut = when {
            read -> StatutMessage.LU
            senderId == monIdentifiant -> StatutMessage.ENVOYE
            else -> StatutMessage.ENVOYE
        },
    )

    /** Analyse les dates ISO 8601 du chat-service (`createdAt`, `lastMessageAt`). */
    private fun parseDate(valeur: String?): Long {
        val texte = valeur?.trim().orEmpty()
        if (texte.isBlank()) return 0L
        for (motif in MOTIFS_DATE) {
            try {
                val format = SimpleDateFormat(motif, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                format.parse(texte)?.let { return it.time }
            } catch (_: ParseException) {
                // Essai du motif suivant.
            }
        }
        return 0L
    }

    private companion object {
        val MOTIFS_DATE = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
        )
    }
}
