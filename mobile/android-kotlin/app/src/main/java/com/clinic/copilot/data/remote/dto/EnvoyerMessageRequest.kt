package com.clinic.copilot.data.remote.dto

import com.squareup.moshi.Json

/**
 * Corps JSON du `POST /conversations/{id}/messages`.
 *
 * Le chat-service n'attend que le texte : l'expéditeur est déduit du JWT côté serveur.
 *
 * @property content Texte du message à envoyer.
 */
data class EnvoyerMessageRequest(
    @param:Json(name = "content") val content: String,
)
