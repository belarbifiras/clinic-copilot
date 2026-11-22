package com.clinic.copilot.domain

/**
 * Message échangé dans une conversation.
 *
 * @property id Identifiant stable du message.
 * @property conversationId Identifiant de la conversation parente.
 * @property expediteurId Identifiant de l'expéditeur ; comparé à l'utilisateur courant pour choisir le sens de la bulle.
 * @property contenu Texte du message.
 * @property dateEnvoi Horodatage d'envoi, en millisecondes Unix.
 * @property statut État de livraison ou de lecture du message.
 */
data class Message(
    val id: String,
    val conversationId: String,
    val expediteurId: String,
    val contenu: String,
    val dateEnvoi: Long,
    val statut: StatutMessage,
)

/**
 * Statut observable d'un message.
 */
enum class StatutMessage {

    /** Message transmis au serveur, pas encore lu par le destinataire. */
    ENVOYE,

    /** Envoi localement échoué : l'utilisateur peut réessayer. */
    ECHEC,

    /** Message lu par le destinataire. */
    LU,
}
