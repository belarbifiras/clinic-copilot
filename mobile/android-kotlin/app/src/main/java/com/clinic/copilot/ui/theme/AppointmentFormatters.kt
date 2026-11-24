package com.clinic.copilot.ui.theme

import com.clinic.copilot.domain.Appointment
import com.clinic.copilot.domain.MedecinsCatalogue
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Case d'une grille de calendrier mensuel (7 colonnes, semaine commençant le lundi).
 *
 * @property numero Numéro du jour affiché.
 * @property horodatage Début de journée correspondant.
 * @property appartientAuMois `false` pour les jours de repli des mois voisins.
 */
data class JourCalendrier(
    val numero: Int,
    val horodatage: Long,
    val appartientAuMois: Boolean,
)

/**
 * Utilitaires de présentation de la feature rendez-vous.
 *
 * Ils réutilisent [ChatFormatters] pour l'initiale des médecins et n'introduisent
 * aucune couleur ou typographie supplémentaire.
 */
object AppointmentFormatters {

    /**
     * Calcule les [nombre] prochains jours, ramenés au début de chaque journée.
     *
     * @param nombre Nombre de jours à retourner.
     * @return Horodatages du jour courant aux jours suivants.
     */
    fun joursSuivants(nombre: Int = NOMBRE_JOURS_PAR_DEFAUT): List<Long> {
        val calendrier = Calendar.getInstance(Locale.FRANCE).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return (0 until nombre).map { decalage ->
            (calendrier.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, decalage)
            }.timeInMillis
        }
    }

    /**
     * Fournit le libellé d'un chip de date : jour abrégé et numéro du jour.
     *
     * @param horodatage Jour concerné.
     * @return Une paire (jour abrégé capitalisé, numéro sur deux chiffres).
     */
    fun formaterJourChip(horodatage: Long): Pair<String, String> {
        val jour = SimpleDateFormat("EEE", Locale.FRENCH).format(Date(horodatage))
        val numero = SimpleDateFormat("dd", Locale.FRENCH).format(Date(horodatage))
        return jour.replaceFirstChar { it.uppercase() } to numero
    }

    /**
     * Formate une date complète lisible.
     *
     * @param horodatage Jour concerné.
     * @return Par exemple « mar. 24 oct. 2024 ».
     */
    fun formaterDateComplete(horodatage: Long): String =
        SimpleDateFormat("EEE dd MMM yyyy", Locale.FRENCH).format(Date(horodatage))
            .replaceFirstChar { it.uppercase() }

    /**
     * Formate une date au format « 9 Novembre », utilisé par les Snackbars.
     *
     * @param horodatage Jour concerné.
     */
    fun formaterJourMois(horodatage: Long): String =
        SimpleDateFormat("d MMMM", Locale.FRENCH).format(Date(horodatage))
            .replaceFirstChar { it.uppercase() }

    /**
     * Formate le titre du calendrier mensuel, par exemple « Novembre 2026 ».
     *
     * @param horodatage Un jour du mois affiché.
     */
    fun formaterMoisAnnee(horodatage: Long): String =
        SimpleDateFormat("MMMM yyyy", Locale.FRENCH).format(Date(horodatage))
            .replaceFirstChar { it.uppercase() }

    /**
     * Formate l'intervalle horaire d'un rendez-vous.
     *
     * @param debut Horodatage de début.
     * @param fin Horodatage de fin.
     * @return Par exemple « 09:00 - 09:30 ».
     */
    fun formaterHeure(debut: Long, fin: Long): String {
        val motif = SimpleDateFormat("HH:mm", Locale.FRANCE)
        return "${motif.format(Date(debut))} - ${motif.format(Date(fin))}"
    }

    /**
     * Formate l'heure seule, utilisée dans les messages de confirmation.
     *
     * @param horodatage Horodatage concerné.
     */
    fun formaterHeureSeule(horodatage: Long): String =
        SimpleDateFormat("HH:mm", Locale.FRANCE).format(Date(horodatage))

    /**
     * Formate la date et l'heure d'un rendez-vous pour une carte ou un récapitulatif.
     *
     * @param rendezVous Rendez-vous concerné.
     * @return Date complète suivie de l'intervalle horaire.
     */
    fun formaterDateHeure(rendezVous: Appointment): String =
        "${formaterDateComplete(rendezVous.date)} · ${formaterHeure(rendezVous.heureDebut, rendezVous.heureFin)}"

    /** Initiale d'un médecin, déléguée au formatter historique de la messagerie. */
    fun initiale(nom: String): String = ChatFormatters.initiale(nom)

    /** Nom affiché dérivé d'un identifiant médecin. */
    fun nomMedecin(doctorId: String): String =
        MedecinsCatalogue.parId(doctorId)?.nom ?: when (doctorId) {
            "1" -> "Dr. Marc Laurent"
            "2" -> "Dr. Sarah Benali"
            else -> "Dr. Médecin"
        }

    /** Spécialité affichée dérivée d'un identifiant médecin. */
    fun specialiteMedecin(doctorId: String): String =
        MedecinsCatalogue.parId(doctorId)?.specialite ?: "Médecin généraliste"

    /** Ramène un horodatage au début de sa journée. */
    fun debutJour(horodatage: Long): Long = Calendar.getInstance(Locale.FRANCE).apply {
        timeInMillis = horodatage
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    /**
     * Décale un premier jour de mois de [delta] mois.
     *
     * @param premierJourMois Horodatage du premier jour du mois courant.
     * @param delta Nombre de mois (négatif pour précédent).
     */
    fun deplacerMois(premierJourMois: Long, delta: Int): Long =
        Calendar.getInstance(Locale.FRANCE).apply {
            timeInMillis = premierJourMois
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, delta)
        }.timeInMillis

    /**
     * Construit une grille de 42 cases (6 semaines) commençant un lundi.
     *
     * @param premierJourMois Premier jour du mois à afficher.
     * @return Les cases du calendrier, les jours hors mois étant marqués.
     */
    fun grilleMois(premierJourMois: Long): List<JourCalendrier> {
        val calendrier = Calendar.getInstance(Locale.FRANCE).apply {
            timeInMillis = premierJourMois
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val moisCourant = calendrier.get(Calendar.MONTH)
        // Calendar : 1 = dimanche ; on veut le lundi comme première colonne.
        val decalageLundi = (calendrier.get(Calendar.DAY_OF_WEEK) + 5) % 7
        calendrier.add(Calendar.DAY_OF_MONTH, -decalageLundi)

        return (0 until NOMBRE_CASES_CALENDRIER).map { _ ->
            val horodatage = calendrier.timeInMillis
            val case = JourCalendrier(
                numero = calendrier.get(Calendar.DAY_OF_MONTH),
                horodatage = horodatage,
                appartientAuMois = calendrier.get(Calendar.MONTH) == moisCourant,
            )
            calendrier.add(Calendar.DAY_OF_MONTH, 1)
            case
        }
    }

    /**
     * Nombre de jours calendaires entre deux horodatages, arrondi au plus proche.
     *
     * @param debut Horodatage de départ.
     * @param fin Horodatage d'arrivée.
     */
    fun nombreJoursEntre(debut: Long, fin: Long): Int =
        ((debutJour(fin) - debutJour(debut)) / MILLISECONDS_PAR_JOUR).toInt()

    private const val NOMBRE_JOURS_PAR_DEFAUT = 7
    private const val NOMBRE_CASES_CALENDRIER = 42
    private const val MILLISECONDS_PAR_JOUR = 24L * 60L * 60L * 1000L
}
