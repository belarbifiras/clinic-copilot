package com.clinic.copilot.data.remote.dto

import com.squareup.moshi.Json

/**
 * Corps JSON renvoyé par `PATCH /conversations/{id}/read`.
 *
 * @property updated Nombre de messages effectivement passés en lu.
 */
data class MarquerLuResponse(
    @param:Json(name = "updated") val updated: Int = 0,
)
