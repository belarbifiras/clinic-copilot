package com.clinic.copilot.domain

/**
 * Médecin proposé dans la carte « Prendre un rendez-vous ».
 *
 * @property id Identifiant stable utilisé pour demander les créneaux au dépôt.
 * @property nom Nom affiché.
 * @property specialite Libellé de spécialité affiché.
 * @property categorie Catégorie du filtre à chips.
 * @property note Note affichée avec le nombre d'avis.
 * @property nombreAvis Nombre d'avis.
 * @property lieu Nom du lieu (ou « Téléconsultation »).
 * @property adresse Adresse postale utilisée par l'Intent Maps.
 * @property teleconsultation `true` pour une consultation à distance.
 */
data class MedecinCatalogue(
    val id: String,
    val nom: String,
    val specialite: String,
    val categorie: SpecialiteRendezVous,
    val note: Float,
    val nombreAvis: Int,
    val lieu: String,
    val adresse: String,
    val teleconsultation: Boolean = false,
)

/**
 * Catalogue local des médecins de la démo, filtrable par spécialité.
 *
 * TODO: remplacer ce catalogue statique par un appel à doctor-directory-service
 * lorsque l'endpoint mobile sera disponible ; le dépôt continue à fournir les créneaux.
 */
object MedecinsCatalogue {

    /** Médecins de la démo, correspondant exactement à la référence Stitch. */
    val tous: List<MedecinCatalogue> = listOf(
        MedecinCatalogue(
            id = "doctor-marc-laurent",
            nom = "Dr. Marc Laurent",
            specialite = "Cardiologie Générale",
            categorie = SpecialiteRendezVous.CARDIOLOGIE,
            note = 4.9f,
            nombreAvis = 128,
            lieu = "Clinique Pasteur",
            adresse = "45 Rue de la Santé 75013 Paris",
        ),
        MedecinCatalogue(
            id = "doctor-sarah-benali",
            nom = "Dr. Sarah Benali",
            specialite = "Consultation de suivi",
            categorie = SpecialiteRendezVous.GENERALISTE,
            note = 4.8f,
            nombreAvis = 94,
            lieu = "Téléconsultation",
            adresse = "",
            teleconsultation = true,
        ),
        MedecinCatalogue(
            id = "doctor-julie-petit",
            nom = "Dr. Julie Petit",
            specialite = "Pédiatrie",
            categorie = SpecialiteRendezVous.PEDIATRIE,
            note = 4.9f,
            nombreAvis = 76,
            lieu = "Cabinet des Enfants",
            adresse = "12 Avenue de la République 75011 Paris",
        ),
    )

    /**
     * Retourne les médecins d'une catégorie donnée.
     *
     * @param categorie Spécialité sélectionnée dans les chips.
     */
    fun parSpecialite(categorie: SpecialiteRendezVous): List<MedecinCatalogue> =
        tous.filter { it.categorie == categorie }

    /**
     * Retrouve un médecin par identifiant.
     *
     * @param id Identifiant du médecin, tel que transmis au dépôt.
     */
    fun parId(id: String): MedecinCatalogue? = tous.firstOrNull { it.id == id }
}
