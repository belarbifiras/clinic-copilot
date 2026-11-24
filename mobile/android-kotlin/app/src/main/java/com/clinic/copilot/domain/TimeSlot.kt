package com.clinic.copilot.domain

/**
 * Créneau de disponibilité d'un médecin, tel qu'affiché dans la grille de l'US-30.
 *
 * @property id Identifiant stable du créneau, utilisé pour réserver ou reprogrammer.
 * @property heureDebut Horodatage du début du créneau, en millisecondes.
 * @property heureFin Horodatage de la fin du créneau, en millisecondes.
 * @property disponible `false` lorsque le créneau est déjà occupé.
 */
data class TimeSlot(
    val id: String,
    val heureDebut: Long,
    val heureFin: Long,
    val disponible: Boolean,
)
