package com.clinic.copilot.data.remote.dto

/**
 * Rendez-vous renvoyé par l'appointment-service.
 *
 * @property id Identifiant numérique du rendez-vous.
 * @property patientId Identifiant du patient.
 * @property doctorId Identifiant du médecin.
 * @property timeSlotId Identifiant du créneau occupé.
 * @property startTime Date ISO locale de début.
 * @property endTime Date ISO locale de fin.
 * @property status Statut brut (`CONFIRMED`, `CANCELLED`, `COMPLETED`, `PENDING`).
 * @property reason Motif éventuel, utilisé notamment pour l'annulation.
 */
data class AppointmentDto(
    val id: Long? = null,
    val patientId: Long? = null,
    val doctorId: Long? = null,
    val timeSlotId: Long? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val status: String? = null,
    val reason: String? = null,
)
