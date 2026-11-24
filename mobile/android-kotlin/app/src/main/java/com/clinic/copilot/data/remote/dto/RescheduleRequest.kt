package com.clinic.copilot.data.remote.dto

/**
 * Corps JSON de `POST /api/appointments/{id}/reschedule`.
 *
 * @property newTimeSlotId Identifiant du nouveau créneau choisi.
 * @property reason Motif facultatif de la reprogrammation.
 */
data class RescheduleRequest(
    val newTimeSlotId: Long,
    val reason: String? = null,
)
