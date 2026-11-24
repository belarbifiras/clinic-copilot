package com.clinic.copilot.data

import com.clinic.copilot.domain.Appointment
import com.clinic.copilot.domain.StatutRendezVous
import com.clinic.copilot.domain.TimeSlot
import kotlinx.coroutines.flow.Flow

/**
 * Contrat d'accès aux créneaux et aux rendez-vous, indépendant de la source de données.
 *
 * L'implémentation active est choisie dans [com.clinic.copilot.di.AppointmentModule].
 */
interface AppointmentRepository {

    /**
     * Observe les créneaux d'un médecin pour une journée.
     *
     * @param doctorId Identifiant du médecin.
     * @param date Début de journée en millisecondes.
     * @return Un flux de créneaux, émis à chaque re-collecte.
     */
    fun observerCreneaux(doctorId: String, date: Long): Flow<List<TimeSlot>>

    /**
     * Réserve un créneau ; l'identité patient est portée par le dépôt réel via le JWT.
     *
     * @param doctorId Identifiant du médecin.
     * @param slotId Identifiant du créneau choisi.
     * @return Le rendez-vous créé.
     */
    suspend fun reserverRendezVous(doctorId: String, slotId: String): Appointment

    /**
     * Observe les rendez-vous du patient courant.
     *
     * @param statut Filtre optionnel envoyé au service ; le filtrage final reste local
     * pour distinguer les rendez-vous confirmés à venir des rendez-vous passés.
     * @return Un flux de rendez-vous.
     */
    fun observerMesRendezVous(statut: StatutRendezVous? = null): Flow<List<Appointment>>

    /**
     * Annule un rendez-vous.
     *
     * @param appointmentId Identifiant du rendez-vous.
     * @param motif Motif saisi par le patient.
     * @return Le rendez-vous avec son statut mis à jour.
     */
    suspend fun annulerRendezVous(appointmentId: String, motif: String): Appointment

    /**
     * Reprogramme un rendez-vous vers un nouveau créneau et libère l'ancien.
     *
     * @param appointmentId Identifiant du rendez-vous.
     * @param nouveauSlotId Identifiant du créneau de remplacement.
     * @return Le rendez-vous reprogrammé.
     */
    suspend fun reprogrammerRendezVous(
        appointmentId: String,
        nouveauSlotId: String,
    ): Appointment
}
