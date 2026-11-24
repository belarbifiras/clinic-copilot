package com.clinic.copilot.data.remote.dto

/**
 * Corps JSON de `POST /api/appointments`.
 *
 * @property patientId Identifiant patient transmis au service.
 * @property doctorId Identifiant du médecin choisi.
 * @property timeSlotId Identifiant du créneau réservé.
 * @property reason Motif facultatif de la consultation.
 */
data class BookAppointmentRequest(
    val patientId: Long,
    val doctorId: Long,
    val timeSlotId: Long,
    val reason: String? = null,
)
