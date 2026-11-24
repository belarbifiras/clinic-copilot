package com.clinic.copilot.data.remote

import com.clinic.copilot.data.remote.dto.AppointmentDto
import com.clinic.copilot.data.remote.dto.BookAppointmentRequest
import com.clinic.copilot.data.remote.dto.RescheduleRequest
import com.clinic.copilot.data.remote.dto.TimeSlotDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Contrat Retrofit de l'appointment-service, exposé par la gateway sous `/api/`.
 *
 * Le client Retrofit est dérivé de celui de [com.clinic.copilot.core.network.NetworkModule] :
 * mêmes OkHttp, Moshi et intercepteur JWT ; seule la base est ajustée ici.
 * Les chemins ci-dessous sont donc relatifs à `/api/`.
 */
interface AppointmentApiService {

    /**
     * Liste les créneaux d'un médecin pour une date donnée.
     *
     * TODO: confirmer le nom du paramètre de filtre `status` avec l'équipe appointment-service.
     *
     * @param doctorId Identifiant numérique du médecin.
     * @param date Date au format `yyyy-MM-dd`.
     * @param statut Filtre optionnel côté serveur (`AVAILABLE` par défaut).
     * @return Les créneaux du jour.
     */
    @GET("availability/doctors/{doctorId}/slots")
    suspend fun obtenirCreneaux(
        @Path("doctorId") doctorId: Long,
        @Query("date") date: String,
        @Query("status") statut: String? = null,
    ): List<TimeSlotDto>

    /**
     * Réserve un créneau pour le patient authentifié.
     *
     * TODO: vérifier si le service passe le patient par le JWT plutôt que par le corps JSON.
     *
     * @param request Identifiants patient, médecin et créneau.
     * @return Le rendez-vous créé.
     */
    @POST("appointments")
    suspend fun reserverRendezVous(@Body request: BookAppointmentRequest): AppointmentDto

    /**
     * Liste les rendez-vous d'un patient, avec filtre de statut optionnel.
     *
     * TODO: `status` est ignoré par le contrôleur actuel ; le dépôt filtre aussi localement.
     *
     * @param patientId Identifiant numérique du patient.
     * @param statut Code de statut backend (`CONFIRMED`, `CANCELLED`, `COMPLETED`).
     * @return Les rendez-vous du patient.
     */
    @GET("appointments/patient/{patientId}")
    suspend fun obtenirRendezVous(
        @Path("patientId") patientId: Long,
        @Query("status") statut: String? = null,
    ): List<AppointmentDto>

    /**
     * Annule un rendez-vous.
     *
     * TODO: la consigne demande PATCH ; le backend livre actuellement POST.
     *
     * @param appointmentId Identifiant du rendez-vous.
     * @param motif Motif affiché côté serveur.
     * @return Le rendez-vous annulé.
     */
    @POST("appointments/{id}/cancel")
    suspend fun annulerRendezVous(
        @Path("id") appointmentId: Long,
        @Query("reason") motif: String,
    ): AppointmentDto

    /**
     * Déplace un rendez-vous vers un nouveau créneau.
     *
     * TODO: la consigne demande PATCH ; le backend livre actuellement POST.
     *
     * @param appointmentId Identifiant du rendez-vous.
     * @param request Identifiant du nouveau créneau.
     * @return Le rendez-vous reprogrammé.
     */
    @POST("appointments/{id}/reschedule")
    suspend fun reprogrammerRendezVous(
        @Path("id") appointmentId: Long,
        @Body request: RescheduleRequest,
    ): AppointmentDto
}
