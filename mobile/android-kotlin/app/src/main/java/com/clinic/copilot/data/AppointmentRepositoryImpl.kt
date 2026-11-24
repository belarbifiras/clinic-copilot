package com.clinic.copilot.data

import com.clinic.copilot.core.session.TokenProvider
import com.clinic.copilot.data.remote.AppointmentApiService
import com.clinic.copilot.data.remote.dto.AppointmentDto
import com.clinic.copilot.data.remote.dto.BookAppointmentRequest
import com.clinic.copilot.data.remote.dto.RescheduleRequest
import com.clinic.copilot.data.remote.dto.TimeSlotDto
import com.clinic.copilot.domain.Appointment
import com.clinic.copilot.domain.MedecinsCatalogue
import com.clinic.copilot.domain.MedecinCatalogue
import com.clinic.copilot.domain.SpecialiteRendezVous
import com.clinic.copilot.domain.StatutRendezVous
import com.clinic.copilot.domain.TimeSlot
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implémentation réelle du dépôt, branchée sur l'appointment-service via [AppointmentApiService].
 *
 * Toutes les erreurs HTTP sont converties en [AppointmentError] avant d'atteindre les ViewModels.
 * Les métadonnées d'affichage (nom, spécialité, lieu, note) sont aujourd'hui dérivées du
 * catalogue local : TODO les lire depuis doctor-directory-service dès que l'endpoint mobile existe.
 */
@Singleton
class AppointmentRepositoryImpl @Inject constructor(
    private val api: AppointmentApiService,
    private val moshi: Moshi,
    private val tokenProvider: TokenProvider,
) : AppointmentRepository {

    /**
     * Observe les créneaux via `GET /availability/doctors/{id}/slots`.
     *
     * @param doctorId Identifiant du médecin.
     * @param date Début de journée en millisecondes.
     * @return Les créneaux du jour, avec disponibilité déduite du statut serveur.
     */
    override fun observerCreneaux(doctorId: String, date: Long): Flow<List<TimeSlot>> = flow {
        val jours = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date(date))
        val creneaux = appeler {
            api.obtenirCreneaux(idNumerique(doctorId, "médecin"), jours)
        }
        emit(creneaux.map { creneau -> creneau.versDomaine() })
    }

    /**
     * Réserve le créneau via `POST /appointments`.
     *
     * @param doctorId Identifiant du médecin.
     * @param slotId Identifiant du créneau choisi.
     * @return Le rendez-vous créé.
     */
    override suspend fun reserverRendezVous(doctorId: String, slotId: String): Appointment {
        val request = BookAppointmentRequest(
            patientId = idPatientCourant(),
            doctorId = idNumerique(doctorId, "médecin"),
            timeSlotId = idNumerique(slotId, "créneau"),
        )
        return appeler { api.reserverRendezVous(request) }.versDomaine(doctorId)
    }

    /**
     * Observe les rendez-vous via `GET /appointments/patient/{id}`.
     *
     * @param statut Filtre optionnel, envoyé puis réappliqué localement.
     * @return Les rendez-vous du patient courant.
     */
    override fun observerMesRendezVous(statut: StatutRendezVous?): Flow<List<Appointment>> = flow {
        val tous = appeler {
            api.obtenirRendezVous(idPatientCourant(), statut?.versCodeApi())
        }.map { it.versDomaine() }
        emit(if (statut == null) tous else tous.filter { it.statut == statut })
    }

    /**
     * Annule via `POST /appointments/{id}/cancel`.
     *
     * TODO: basculer en PATCH lorsque le backend alignera son contrat.
     *
     * @param appointmentId Identifiant du rendez-vous.
     * @param motif Motif transmis comme paramètre `reason`.
     * @return Le rendez-vous annulé.
     */
    override suspend fun annulerRendezVous(appointmentId: String, motif: String): Appointment =
        appeler {
            api.annulerRendezVous(idNumerique(appointmentId, "rendez-vous"), motif)
        }.versDomaine()

    /**
     * Reprogramme via `POST /appointments/{id}/reschedule`.
     *
     * TODO: basculer en PATCH lorsque le backend alignera son contrat.
     *
     * @param appointmentId Identifiant du rendez-vous.
     * @param nouveauSlotId Identifiant du créneau de remplacement.
     * @return Le rendez-vous reprogrammé.
     */
    override suspend fun reprogrammerRendezVous(
        appointmentId: String,
        nouveauSlotId: String,
    ): Appointment = appeler {
        api.reprogrammerRendezVous(
            idNumerique(appointmentId, "rendez-vous"),
            RescheduleRequest(newTimeSlotId = idNumerique(nouveauSlotId, "créneau")),
        )
    }.versDomaine()

    /** Exécute un appel réseau et convertit ses erreurs en [AppointmentError]. */
    private suspend fun <T> appeler(bloc: suspend () -> T): T = try {
        bloc()
    } catch (annulation: CancellationException) {
        throw annulation
    } catch (cause: Exception) {
        throw cause.commeAppointmentError(moshi)
    }

    /**
     * Convertit un identifiant texte en identifiant numérique attendu par le backend.
     *
     * Les identifiants de démonstration sont de la forme `doctor-marc-laurent` ; l'ordre du
     * catalogue fournit alors un identifiant numérique stable (TODO: retirer une fois le
     * service d'annuaire médical branché côté mobile).
     */
    private fun idNumerique(valeur: String, libelle: String): Long =
        valeur.substringAfterLast('-').toLongOrNull()
            ?: valeur.toLongOrNull()
            ?: MedecinsCatalogue.tous.indexOfFirst { it.id == valeur }
                .takeIf { it >= 0 }?.let { (it + 1).toLong() }
            ?: throw AppointmentError.Refuse("Identifiant $libelle invalide.")

    /**
     * Identifiant patient transmis au service.
     *
     * TODO: remplacer le repli par l'identifiant issu de la session authentifiée
     * lorsque la feature auth fournira un identifiant numérique.
     */
    private fun idPatientCourant(): Long =
        tokenProvider.obtenirUtilisateurId().toLongOrNull() ?: PATIENT_DEMO_ID

    /** Convertit un créneau JSON en modèle métier. */
    private fun TimeSlotDto.versDomaine(): TimeSlot {
        val debut = parseDate(startTime)
        return TimeSlot(
            id = id?.toString() ?: UUID.randomUUID().toString(),
            heureDebut = debut,
            heureFin = parseDate(endTime).takeIf { it != 0L } ?: debut + DUREE_PAR_DEFAUT_MS,
            disponible = status.isNullOrBlank() || status.contains("AVAIL", ignoreCase = true) ||
                status.contains("LIBRE", ignoreCase = true),
        )
    }

    /** Convertit un rendez-vous JSON en modèle métier, enrichi depuis le catalogue local. */
    private fun AppointmentDto.versDomaine(doctorIdFallback: String? = null): Appointment {
        val debut = parseDate(startTime)
        val identifiantMedecin = doctorId?.toString() ?: doctorIdFallback.orEmpty()
        val medecin = medecinDepuisId(identifiantMedecin)
        return Appointment(
            id = id?.toString() ?: UUID.randomUUID().toString(),
            doctorId = identifiantMedecin,
            doctorName = medecin?.nom ?: "Dr. Médecin",
            specialite = medecin?.specialite ?: "Médecin généraliste",
            date = debutJour(debut),
            heureDebut = debut,
            heureFin = parseDate(endTime).takeIf { it != 0L } ?: debut + DUREE_PAR_DEFAUT_MS,
            statut = status?.versStatut() ?: StatutRendezVous.CONFIRME,
            lieu = medecin?.lieu,
            adresse = medecin?.adresse?.takeIf { it.isNotBlank() },
            categorie = medecin?.categorie ?: SpecialiteRendezVous.GENERALISTE,
            note = medecin?.note ?: 4.9f,
            nombreAvis = medecin?.nombreAvis ?: 0,
            // TODO: champ non encore exposé par l'API ; brancher sur le service de documents.
            compteRenduDisponible = false,
            teleconsultation = medecin?.teleconsultation ?: false,
        )
    }

    /**
     * Retrouve le médecin correspondant à un identifiant du service.
     *
     * TODO: remplacer ce repli catalogue par un appel doctor-directory-service.
     */
    private fun medecinDepuisId(doctorId: String): MedecinCatalogue? =
        MedecinsCatalogue.parId(doctorId)
            ?: doctorId.toLongOrNull()?.let { MedecinsCatalogue.tous.getOrNull((it - 1).toInt()) }

    /** Mappe le statut brut du service vers l'enum métier, y compris les demandes en attente. */
    private fun String.versStatut(): StatutRendezVous = when (uppercase(Locale.US)) {
        "CANCELLED", "CANCELED" -> StatutRendezVous.ANNULE
        "COMPLETED" -> StatutRendezVous.TERMINE
        "PENDING", "EN_ATTENTE" -> StatutRendezVous.EN_ATTENTE
        else -> StatutRendezVous.CONFIRME
    }

    /** Mappe l'enum métier vers le code attendu par le service. */
    private fun StatutRendezVous.versCodeApi(): String = when (this) {
        StatutRendezVous.CONFIRME -> "CONFIRMED"
        StatutRendezVous.EN_ATTENTE -> "PENDING"
        StatutRendezVous.ANNULE -> "CANCELLED"
        StatutRendezVous.TERMINE -> "COMPLETED"
    }

    /** Analyse les dates ISO locales renvoyées par le service. */
    private fun parseDate(valeur: String?): Long {
        val texte = valeur?.trim().orEmpty()
        if (texte.isBlank()) return 0L
        for (motif in MOTIFS_DATE) {
            try {
                SimpleDateFormat(motif, Locale.US).parse(texte)?.let { return it.time }
            } catch (_: ParseException) {
                // Essai du motif suivant.
            }
        }
        return 0L
    }

    /** Ramène un horodatage au début de sa journée. */
    private fun debutJour(horodatage: Long): Long = Calendar.getInstance(Locale.FRANCE).apply {
        timeInMillis = horodatage
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private companion object {
        const val PATIENT_DEMO_ID = 1L
        const val DUREE_PAR_DEFAUT_MS = 30L * 60L * 1000L
        val MOTIFS_DATE = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
        )
    }
}
