package com.clinic.copilot.data

import com.clinic.copilot.domain.ContactFiche
import com.clinic.copilot.domain.Conversation
import com.clinic.copilot.domain.Message
import com.clinic.copilot.domain.StatutMessage
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Dépôt de démonstration en mémoire, avec délai simulé et données médicales fictives.
 *
 * Il est sélectionné par défaut dans [com.clinic.copilot.di.ChatModule] afin que la
 * feature soit démontrable sans serveur ; le [ChatRepositoryImpl] reste disponible.
 */
@Singleton
class FakeChatRepository @Inject constructor() : ChatRepository {

    private val mutex = Mutex()
    private val maintenant = System.currentTimeMillis()
    private val conversations = MutableStateFlow(creerConversations(maintenant))
    private val historiques = MutableStateFlow(creerHistoriques(maintenant))
    private val nouveauxMessages = MutableSharedFlow<Message>(extraBufferCapacity = 64)

    /**
     * Observe la liste en mémoire après un court délai simulant le réseau.
     *
     * @return Un flux de conversations.
     */
    override fun observerConversations(): Flow<List<Conversation>> = flow {
        delay(DELAI_RESEAU_MS)
        emitAll(conversations)
    }

    /**
     * Retourne une page d'historique en ordre chronologique.
     *
     * @param conversationId Identifiant de la conversation.
     * @param page Numéro de page basé sur 1.
     * @param limit Taille maximale de la page.
     * @return Les messages de la page demandée.
     */
    override suspend fun chargerHistorique(
        conversationId: String,
        page: Int,
        limit: Int,
    ): List<Message> {
        require(page >= 1) { "Le numéro de page doit être supérieur ou égal à 1." }
        require(limit > 0) { "La taille de page doit être supérieure à zéro." }
        delay(DELAI_RESEAU_MS)
        val chronologiques = historiques.value[conversationId].orEmpty().sortedBy(Message::dateEnvoi)
        return chronologiques
            .asReversed()
            .drop((page - 1) * limit)
            .take(limit)
            .asReversed()
    }

    /**
     * Ajoute un message à l'historique en mémoire et le publie sur le flux temps réel.
     *
     * @param conversationId Identifiant de la conversation.
     * @param expediteurId Identifiant de l'utilisateur courant.
     * @param contenu Texte du message.
     * @return Le message créé.
     */
    override suspend fun envoyerMessage(
        conversationId: String,
        expediteurId: String,
        contenu: String,
    ): Message {
        val texte = contenu.trim()
        require(texte.isNotEmpty()) { "Le message ne peut pas être vide." }

        delay(DELAI_RESEAU_MS)
        val message = Message(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            expediteurId = expediteurId,
            contenu = texte,
            dateEnvoi = System.currentTimeMillis(),
            statut = StatutMessage.ENVOYE,
        )
        mutex.withLock {
            historiques.value = historiques.value + (
                conversationId to (historiques.value[conversationId].orEmpty() + message)
                )
            mettreAJourDernierMessage(message)
        }
        nouveauxMessages.emit(message)
        return message
    }

    /**
     * Met à zéro le compteur de messages non lus de la conversation.
     *
     * @param conversationId Identifiant de la conversation.
     */
    override suspend fun marquerCommeLu(conversationId: String) {
        delay(DELAI_RESEAU_MS)
        mutex.withLock {
            conversations.value = conversations.value.map { conversation ->
                if (conversation.id == conversationId) {
                    conversation.copy(nombreNonLus = 0)
                } else {
                    conversation
                }
            }
        }
    }

    /**
     * Retourne la fiche fictive du contact, utilisée par l'Intent explicite.
     *
     * @param conversationId Identifiant de la conversation.
     * @return Nom, spécialité et téléphone fictif du contact.
     */
    override suspend fun obtenirFicheContact(conversationId: String): ContactFiche {
        delay(DELAI_RESEAU_MS)
        return FICHES_CONTACTS[conversationId] ?: ContactFiche(
            nom = "Cabinet médical des Tilleuls",
            specialite = "Secrétariat",
            telephone = "01 45 00 00 00",
        )
    }

    /**
     * Observe les messages publiés après l'abonnement pour une conversation donnée.
     *
     * @param conversationId Identifiant de la conversation.
     * @return Un flux des nouveaux messages correspondants.
     */
    override fun observerMessagesTempsReel(conversationId: String): Flow<Message> = flow {
        delay(DELAI_RESEAU_MS)
        emitAll(nouveauxMessages.filter { it.conversationId == conversationId })
    }

    private fun mettreAJourDernierMessage(message: Message) {
        conversations.value = conversations.value
            .map { conversation ->
                if (conversation.id == message.conversationId) {
                    conversation.copy(
                        dernierMessage = message.contenu,
                        dateDernierMessage = message.dateEnvoi,
                    )
                } else {
                    conversation
                }
            }
            .sortedByDescending(Conversation::dateDernierMessage)
    }

    private fun creerConversations(now: Long): List<Conversation> = listOf(
        Conversation(
            id = "conv-001",
            nomContact = "Dr. Léa Martin",
            dernierMessage = "Merci, nous faisons le point demain.",
            dateDernierMessage = now - 12 * MINUTE_MS,
            nombreNonLus = 2,
        ),
        Conversation(
            id = "conv-002",
            nomContact = "Dr. Thomas Bernard",
            dernierMessage = "Votre ordonnance est disponible.",
            dateDernierMessage = now - 3 * HEURE_MS,
            nombreNonLus = 1,
        ),
        Conversation(
            id = "conv-003",
            nomContact = "Cabinet médical des Tilleuls",
            dernierMessage = "Rendez-vous confirmé pour jeudi à 10 h.",
            dateDernierMessage = now - 1 * JOUR_MS,
            nombreNonLus = 0,
        ),
        Conversation(
            id = "conv-004",
            nomContact = "Dr. Nadia Benali",
            dernierMessage = "N'hésitez pas à nous contacter si besoin.",
            dateDernierMessage = now - 4 * JOUR_MS,
            nombreNonLus = 0,
        ),
    )

    private fun creerHistoriques(now: Long): Map<String, List<Message>> = mapOf(
        "conv-001" to listOf(
            message("conv-001", "patient-demo", "Bonjour docteure, ma toux persiste depuis hier.", now - 42 * MINUTE_MS),
            message("conv-001", "doctor-101", "Avez-vous également de la fièvre ?", now - 35 * MINUTE_MS),
            message("conv-001", "patient-demo", "Non, pas de fièvre pour le moment.", now - 28 * MINUTE_MS),
            message("conv-001", "doctor-101", "Merci, nous faisons le point demain.", now - 12 * MINUTE_MS),
        ),
        "conv-002" to listOf(
            message("conv-002", "patient-demo", "Bonjour, pouvez-vous renouveler mon ordonnance ?", now - 5 * HEURE_MS),
            message("conv-002", "doctor-102", "Oui, je vérifie votre dossier.", now - 4 * HEURE_MS),
            message("conv-002", "doctor-102", "Votre ordonnance est disponible.", now - 3 * HEURE_MS),
        ),
        "conv-003" to listOf(
            message("conv-003", "patient-demo", "Je confirme ma venue jeudi.", now - 2 * JOUR_MS),
            message("conv-003", "clinic-201", "Rendez-vous confirmé pour jeudi à 10 h.", now - 1 * JOUR_MS),
        ),
        "conv-004" to listOf(
            message("conv-004", "doctor-103", "Les résultats sont rassurants.", now - 5 * JOUR_MS),
            message("conv-004", "patient-demo", "Merci pour votre retour.", now - 4 * JOUR_MS),
            message("conv-004", "doctor-103", "N'hésitez pas à nous contacter si besoin.", now - 4 * JOUR_MS + MINUTE_MS),
        ),
    )

    private fun message(
        conversationId: String,
        senderId: String,
        content: String,
        sentAt: Long,
    ) = Message(
        id = UUID.randomUUID().toString(),
        conversationId = conversationId,
        expediteurId = senderId,
        contenu = content,
        dateEnvoi = sentAt,
        statut = if (senderId == "patient-demo") StatutMessage.LU else StatutMessage.ENVOYE,
    )

    private companion object {
        const val DELAI_RESEAU_MS = 350L
        const val MINUTE_MS = 60_000L
        const val HEURE_MS = 60 * MINUTE_MS
        const val JOUR_MS = 24 * HEURE_MS

        val FICHES_CONTACTS = mapOf(
            "conv-001" to ContactFiche("Dr. Léa Martin", "Médecin généraliste", "01 42 10 20 30"),
            "conv-002" to ContactFiche("Dr. Thomas Bernard", "Cardiologue", "01 43 20 30 40"),
            "conv-003" to ContactFiche("Cabinet médical des Tilleuls", "Secrétariat", "01 45 00 00 00"),
            "conv-004" to ContactFiche("Dr. Nadia Benali", "Dermatologue", "01 46 30 40 50"),
        )
    }
}
