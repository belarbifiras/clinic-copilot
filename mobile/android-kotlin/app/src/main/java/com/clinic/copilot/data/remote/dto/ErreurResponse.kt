package com.clinic.copilot.data.remote.dto

import com.squareup.moshi.Json

/**
 * Corps d'erreur JSON du chat-service : `{"message": "..."}`.
 *
 * @property message Message d'erreur renvoyé par le serveur, affiché à l'utilisateur.
 */
data class ErreurResponse(
    @param:Json(name = "message") val message: String? = null,
)
