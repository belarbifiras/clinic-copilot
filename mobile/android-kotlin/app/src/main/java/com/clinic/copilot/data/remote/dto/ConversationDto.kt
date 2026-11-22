package com.clinic.copilot.data.remote.dto

import com.squareup.moshi.Json

/**
 * Représentation JSON renvoyée par `GET /conversations` du chat-service.
 *
 * Les champs `contactName` et `unreadCount` sont optionnels : la gateway peut les
 * enrichir plus tard, le chat-service ne les expose pas encore.
 *
 * @property id Champ `_id` Mongo renvoyé par l'API.
 * @property patientId Identifiant du patient de la conversation.
 * @property doctorId Identifiant du praticien de la conversation.
 * @property lastMessage Aperçu du dernier message (`lastMessage`).
 * @property lastMessageAt Horodatage ISO 8601 du dernier message.
 * @property updatedAt Horodatage ISO 8601 de la dernière mise à jour.
 * @property contactName Nom du contact si la gateway le fournit.
 * @property unreadCount Nombre de non-lus si la gateway le fournit.
 */
data class ConversationDto(
    @param:Json(name = "_id") val id: String = "",
    @param:Json(name = "patientId") val patientId: String? = null,
    @param:Json(name = "doctorId") val doctorId: String? = null,
    @param:Json(name = "lastMessage") val lastMessage: String? = null,
    @param:Json(name = "lastMessageAt") val lastMessageAt: String? = null,
    @param:Json(name = "updatedAt") val updatedAt: String? = null,
    @param:Json(name = "contactName") val contactName: String? = null,
    @param:Json(name = "unreadCount") val unreadCount: Int? = null,
)
