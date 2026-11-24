package com.clinic.copilot.data

import com.clinic.copilot.domain.Appointment
import com.clinic.copilot.domain.MedecinsCatalogue
import com.clinic.copilot.domain.SpecialiteRendezVous
import com.clinic.copilot.domain.StatutRendezVous
import com.clinic.copilot.domain.TimeSlot
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dépôt de démonstration en mémoire, avec délai simulé et données exactes de la maquette.
 *
 * Il est sélectionné par défaut dans [com.clinic.copilot.di.AppointmentModule] afin que le
 * parcours soit testable sans backend ; [AppointmentRepositoryImpl] reste disponible.
 */
@Singleton
class FakeAppointmentRepository @Inject constructor() : AppointmentRepository {

    private val mutex = Mutex()
    private val donnees = construireDonnees()
    private val creneaux = MutableStateFlow(donnees.creneaux)
    private val rendezVous = MutableStateFlow(donnees.rendezVous)
    private val slotsParRendezVous = donnees.slotsParRendezVous.toMutableMap()
    private val conflitsSimules = mutableSetOf(donnees.slotConflitId)

    /**
     * Observe les créneaux d'un médecin pour une journée.
     *
     * @param doctorId Identifiant du médecin.
     * @param date Début de journée en millisecondes.
     * @return Les créneaux du médecin pour cette date.
     */
    override fun observerCreneaux(doctorId: String, date: Long): Flow<List<TimeSlot>> = flow {
        delay(DELAI_RESEAU_MS)
        emit(
            creneaux.value.filter { creneau ->
                creneau.id.startsWith("slot-$doctorId-") && debutJour(creneau.heureDebut) == date
            },
        )
    }

    /**
     * Réserve un créneau, avec un conflit simulé une seule fois pour la démo du 409.
     *
     * @param doctorId Identifiant du médecin.
     * @param slotId Identifiant du créneau choisi.
     * @return Le rendez-vous créé.
     */
    override suspend fun reserverRendezVous(doctorId: String, slotId: String): Appointment {
        delay(DELAI_RESEAU_MS)
        return mutex.withLock {
            val creneau = creneaux.value.firstOrNull { it.id == slotId }
                ?: throw AppointmentError.Introuvable
            if (!creneau.disponible || slotId in conflitsSimules) {
                conflitsSimules.remove(slotId)
                creneaux.value = creneaux.value.map {
                    if (it.id == slotId) it.copy(disponible = false) else it
                }
                throw AppointmentError.Conflit()
            }

            val medecin = MedecinsCatalogue.parId(doctorId)
                ?: throw AppointmentError.Introuvable
            val nouveau = Appointment(
                id = "rdv-${UUID.randomUUID()}",
                doctorId = medecin.id,
                doctorName = medecin.nom,
                specialite = medecin.specialite,
                date = debutJour(creneau.heureDebut),
                heureDebut = creneau.heureDebut,
                heureFin = creneau.heureFin,
                statut = StatutRendezVous.CONFIRME,
                lieu = medecin.lieu,
                adresse = medecin.adresse,
                categorie = medecin.categorie,
                note = medecin.note,
                nombreAvis = medecin.nombreAvis,
                teleconsultation = medecin.teleconsultation,
            )
            creneaux.value = creneaux.value.map {
                if (it.id == slotId) it.copy(disponible = false) else it
            }
            rendezVous.value = rendezVous.value + nouveau
            slotsParRendezVous[nouveau.id] = slotId
            nouveau
        }
    }

    /**
     * Observe les rendez-vous du patient, tous statuts confondus.
     *
     * @param statut Filtre optionnel appliqué côté mémoire.
     * @return Les rendez-vous correspondants.
     */
    override fun observerMesRendezVous(statut: StatutRendezVous?): Flow<List<Appointment>> = flow {
        delay(DELAI_RESEAU_MS)
        val liste = if (statut == null) {
            rendezVous.value
        } else {
            rendezVous.value.filter { it.statut == statut }
        }
        emit(liste)
    }

    /**
     * Annule un rendez-vous et libère son créneau.
     *
     * @param appointmentId Identifiant du rendez-vous.
     * @param motif Motif ignoré par le dépôt factice, reçu pour coller au contrat réel.
     * @return Le rendez-vous annulé.
     */
    override suspend fun annulerRendezVous(appointmentId: String, motif: String): Appointment {
        delay(DELAI_RESEAU_MS)
        return mutex.withLock {
            val courant = rendezVous.value.firstOrNull { it.id == appointmentId }
                ?: throw AppointmentError.Introuvable
            if (courant.statut != StatutRendezVous.CONFIRME &&
                courant.statut != StatutRendezVous.EN_ATTENTE
            ) {
                throw AppointmentError.Refuse("Ce rendez-vous ne peut plus être annulé.")
            }

            val modifie = courant.copy(statut = StatutRendezVous.ANNULE)
            rendezVous.value = rendezVous.value.map { if (it.id == appointmentId) modifie else it }
            val ancienSlot = slotsParRendezVous.remove(appointmentId)
            if (ancienSlot != null) {
                creneaux.value = creneaux.value.map {
                    if (it.id == ancienSlot) it.copy(disponible = true) else it
                }
            }
            modifie
        }
    }

    /**
     * Déplace un rendez-vous et libère l'ancien créneau.
     *
     * @param appointmentId Identifiant du rendez-vous.
     * @param nouveauSlotId Identifiant du créneau de remplacement.
     * @return Le rendez-vous reprogrammé.
     */
    override suspend fun reprogrammerRendezVous(
        appointmentId: String,
        nouveauSlotId: String,
    ): Appointment {
        delay(DELAI_RESEAU_MS)
        return mutex.withLock {
            val courant = rendezVous.value.firstOrNull { it.id == appointmentId }
                ?: throw AppointmentError.Introuvable
            if (courant.statut != StatutRendezVous.CONFIRME &&
                courant.statut != StatutRendezVous.EN_ATTENTE
            ) {
                throw AppointmentError.Refuse("Seul un rendez-vous actif peut être reprogrammé.")
            }
            val nouveauCreneau = creneaux.value.firstOrNull { it.id == nouveauSlotId }
                ?: throw AppointmentError.Introuvable
            if (!nouveauCreneau.disponible || nouveauSlotId in conflitsSimules) {
                conflitsSimules.remove(nouveauSlotId)
                creneaux.value = creneaux.value.map {
                    if (it.id == nouveauSlotId) it.copy(disponible = false) else it
                }
                throw AppointmentError.Conflit()
            }

            val ancienSlot = slotsParRendezVous[appointmentId]
            creneaux.value = creneaux.value.map { creneau ->
                when (creneau.id) {
                    ancienSlot -> creneau.copy(disponible = true)
                    nouveauCreneau.id -> creneau.copy(disponible = false)
                    else -> creneau
                }
            }
            val modifie = courant.copy(
                date = debutJour(nouveauCreneau.heureDebut),
                heureDebut = nouveauCreneau.heureDebut,
                heureFin = nouveauCreneau.heureFin,
            )
            rendezVous.value = rendezVous.value.map { if (it.id == appointmentId) modifie else it }
            slotsParRendezVous[appointmentId] = nouveauSlotId
            modifie
        }
    }

    private data class DonneesInitiales(
        val creneaux: List<TimeSlot>,
        val rendezVous: List<Appointment>,
        val slotsParRendezVous: Map<String, String>,
        val slotConflitId: String,
    )

    /** Construit les rendez-vous exacts de la maquette et les créneaux réservables. */
    private fun construireDonnees(): DonneesInitiales {
        val rendezVous = listOf(
            Appointment(
                id = "rdv-marc-laurent-a-venir",
                doctorId = "doctor-marc-laurent",
                doctorName = "Dr. Marc Laurent",
                specialite = "Cardiologie Générale",
                date = horodatage(2024, Calendar.OCTOBER, 24),
                heureDebut = horodatage(2024, Calendar.OCTOBER, 24, 14, 30),
                heureFin = horodatage(2024, Calendar.OCTOBER, 24, 15, 0),
                statut = StatutRendezVous.CONFIRME,
                lieu = "Clinique Pasteur",
                adresse = "45 Rue de la Santé 75013 Paris",
                categorie = SpecialiteRendezVous.CARDIOLOGIE,
                note = 4.9f,
                nombreAvis = 128,
            ),
            Appointment(
                id = "rdv-sarah-benali-attente",
                doctorId = "doctor-sarah-benali",
                doctorName = "Dr. Sarah Benali",
                specialite = "Consultation de suivi",
                date = horodatage(2024, Calendar.NOVEMBER, 10),
                heureDebut = horodatage(2024, Calendar.NOVEMBER, 10, 9, 15),
                heureFin = horodatage(2024, Calendar.NOVEMBER, 10, 9, 45),
                statut = StatutRendezVous.EN_ATTENTE,
                lieu = "Téléconsultation",
                categorie = SpecialiteRendezVous.GENERALISTE,
                note = 4.8f,
                nombreAvis = 94,
                teleconsultation = true,
            ),
            Appointment(
                id = "rdv-anne-robert-passe",
                doctorId = "doctor-anne-robert",
                doctorName = "Dr. Anne Robert",
                specialite = "Dermatologie - Consultation annuelle",
                date = horodatage(2024, Calendar.SEPTEMBER, 12),
                heureDebut = horodatage(2024, Calendar.SEPTEMBER, 12, 10, 0),
                heureFin = horodatage(2024, Calendar.SEPTEMBER, 12, 10, 30),
                statut = StatutRendezVous.TERMINE,
                lieu = "Cabinet Dermatologique",
                adresse = "8 Rue Victor Hugo 75016 Paris",
                categorie = SpecialiteRendezVous.GENERALISTE,
                compteRenduDisponible = true,
            ),
            Appointment(
                id = "rdv-marc-laurent-annule",
                doctorId = "doctor-marc-laurent",
                doctorName = "Dr. Marc Laurent",
                specialite = "Cardiologie - Bilan d'effort",
                date = horodatage(2024, Calendar.MAY, 4),
                heureDebut = horodatage(2024, Calendar.MAY, 4, 11, 0),
                heureFin = horodatage(2024, Calendar.MAY, 4, 11, 30),
                statut = StatutRendezVous.ANNULE,
                lieu = "Clinique Pasteur",
                adresse = "45 Rue de la Santé 75013 Paris",
                categorie = SpecialiteRendezVous.CARDIOLOGIE,
                note = 4.9f,
                nombreAvis = 128,
            ),
        )

        val creneaux = mutableListOf<TimeSlot>()
        MedecinsCatalogue.tous.forEachIndexed { indexMedecin, medecin ->
            (0 until NOMBRE_JOURS_CREAUX).forEach { decalage ->
                val jour = Calendar.getInstance(Locale.FRANCE).apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    add(Calendar.DAY_OF_YEAR, decalage)
                }.timeInMillis
                HORAIRES.forEachIndexed { indexHoraire, (heure, minute) ->
                    // Quelques créneaux déjà pris, répartis de façon déterministe.
                    if ((decalage + indexMedecin + indexHoraire) % 9 == 0) return@forEachIndexed
                    val debut = instantDuJour(jour, heure, minute)
                    creneaux += TimeSlot(
                        id = "slot-${medecin.id}-${
                            SimpleDateFormat(FormatMotif.JOUR_ID, Locale.US).format(java.util.Date(jour))
                        }-$heure-$minute",
                        heureDebut = debut,
                        heureFin = debut + DUREE_CRENEAU_MS,
                        disponible = true,
                    )
                }
            }
        }

        val slotConflit = creneaux.firstOrNull { it.disponible }
            ?: throw IllegalStateException("Aucun créneau de démonstration disponible")

        return DonneesInitiales(
            creneaux = creneaux,
            rendezVous = rendezVous,
            slotsParRendezVous = emptyMap(),
            slotConflitId = slotConflit.id,
        )
    }

    /** Horodatage d'un jour de la maquette, sans heure. */
    private fun horodatage(annee: Int, mois: Int, jour: Int): Long = Calendar.getInstance(Locale.FRANCE).apply {
        clear()
        set(annee, mois, jour, 0, 0, 0)
    }.timeInMillis

    /** Horodatage d'un jour de la maquette avec une heure. */
    private fun horodatage(annee: Int, mois: Int, jour: Int, heure: Int, minute: Int): Long =
        Calendar.getInstance(Locale.FRANCE).apply {
            clear()
            set(annee, mois, jour, heure, minute, 0)
        }.timeInMillis

    /** Construit un horodatage pour un jour et une heure donnés. */
    private fun instantDuJour(debutJournee: Long, heure: Int, minute: Int): Long =
        Calendar.getInstance(Locale.FRANCE).apply {
            timeInMillis = debutJournee
            set(Calendar.HOUR_OF_DAY, heure)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    /** Ramène un horodatage au début de sa journée. */
    private fun debutJour(horodatage: Long): Long = Calendar.getInstance(Locale.FRANCE).apply {
        timeInMillis = horodatage
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private object FormatMotif {
        const val JOUR_ID = "yyyyMMdd"
    }

    private companion object {
        const val DELAI_RESEAU_MS = 450L
        const val DUREE_CRENEAU_MS = 30L * 60L * 1000L
        const val NOMBRE_JOURS_CREAUX = 60
        val HORAIRES = listOf(
            9 to 0, 9 to 15, 9 to 30, 10 to 0, 10 to 30, 11 to 0,
            14 to 0, 14 to 30, 15 to 0, 15 to 30, 16 to 0,
        )
    }
}
