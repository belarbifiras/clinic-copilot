package com.clinic.copilot.data.remote.dto

/**
 * Créneau renvoyé par `GET /api/availability/doctors/{id}/slots`.
 *
 * @property id Identifiant numérique du créneau.
 * @property doctorId Identifiant du médecin, conservé pour le diagnostic.
 * @property startTime Date ISO locale au format `yyyy-MM-dd'T'HH:mm:ss`.
 * @property endTime Date ISO locale de fin.
 * @property status Statut brut du service (`AVAILABLE`, `BOOKED`, ...).
 */
data class TimeSlotDto(
    val id: Long? = null,
    val doctorId: Long? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val status: String? = null,
)
