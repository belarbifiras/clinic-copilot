package com.clinic.copilot.data.remote.dto

import com.squareup.moshi.Json

/**
 * Représentation JSON d'un message renvoyé par le chat-service.
 *
 * @property id Champ `_id` Mongo renvoyé par l'API.
 * @property conversationId Identifiant de la conversation parente.
 * @property senderId Identifiant de l'expéditeur (`senderId`).
 * @property content Texte du message.
 * @property read Booléen `read` du schéma Mongo, utilisé pour déduire le statut `LU`.
 * @property createdAt Horodatage ISO 8601 de création (`createdAt`).
 */
data class MessageDto(
    @param:Json(name = "_id") val id: String = "",
    @param:Json(name = "conversationId") val conversationId: String? = null,
    @param:Json(name = "senderId") val senderId: String? = null,
    @param:Json(name = "content") val content: String? = null,
    @param:Json(name = "read") val read: Boolean = false,
    @param:Json(name = "createdAt") val createdAt: String? = null,
)
