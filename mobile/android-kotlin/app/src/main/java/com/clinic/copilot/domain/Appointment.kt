package com.clinic.copilot.domain

/**
 * Statut métier d'un rendez-vous patient.
 */
enum class StatutRendezVous {

    /** Rendez-vous confirmé et actif. */
    CONFIRME,

    /** Demande de rendez-vous en attente de confirmation du secrétariat. */
    EN_ATTENTE,

    /** Rendez-vous annulé, conservé dans l'historique. */
    ANNULE,

    /** Rendez-vous passé et terminé. */
    TERMINE,
}

/**
 * Catégorie de spécialité utilisée par les chips de la carte de réservation.
 */
enum class SpecialiteRendezVous(val libelle: String) {

    /** Cardiologie. */
    CARDIOLOGIE("Cardiologie"),

    /** Médecine générale. */
    GENERALISTE("Généraliste"),

    /** Pédiatrie. */
    PEDIATRIE("Pédiatrie"),
}

/**
 * Rendez-vous patient affiché dans l'écran « Mes rendez-vous ».
 *
 * @property id Identifiant du rendez-vous côté serveur.
 * @property doctorId Identifiant du médecin, conservé pour la reprogrammation.
 * @property doctorName Nom affiché du médecin.
 * @property specialite Spécialité affichée sous le nom.
 * @property date Début de journée du rendez-vous, en millisecondes.
 * @property heureDebut Horodatage de début, en millisecondes.
 * @property heureFin Horodatage de fin, en millisecondes.
 * @property statut Statut courant du rendez-vous.
 * @property lieu Lieu du rendez-vous, lorsqu'il est connu.
 * @property adresse Adresse postale utilisée par l'Intent Google Maps.
 * @property categorie Catégorie de spécialité pour le filtre de réservation.
 * @property note Note du praticien affichée sur la carte featured.
 * @property nombreAvis Nombre d'avis associé à [note].
 * @property compteRenduDisponible `true` lorsqu'un compte-rendu peut être téléchargé.
 * @property teleconsultation `true` pour une consultation à distance.
 */
data class Appointment(
    val id: String,
    val doctorId: String,
    val doctorName: String,
    val specialite: String,
    val date: Long,
    val heureDebut: Long,
    val heureFin: Long,
    val statut: StatutRendezVous,
    val lieu: String? = null,
    val adresse: String? = null,
    val categorie: SpecialiteRendezVous = SpecialiteRendezVous.GENERALISTE,
    val note: Float = 4.9f,
    val nombreAvis: Int = 128,
    val compteRenduDisponible: Boolean = false,
    val teleconsultation: Boolean = false,
)
