package com.clinic.copilot.ui.myappointments

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clinic.copilot.data.AppointmentError
import com.clinic.copilot.data.AppointmentRepository
import com.clinic.copilot.data.local.AppointmentPreferences
import com.clinic.copilot.domain.Appointment
import com.clinic.copilot.domain.MedecinCatalogue
import com.clinic.copilot.domain.MedecinsCatalogue
import com.clinic.copilot.domain.SpecialiteRendezVous
import com.clinic.copilot.domain.StatutRendezVous
import com.clinic.copilot.domain.TimeSlot
import com.clinic.copilot.ui.theme.AppointmentFormatters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

/** Onglets de l'écran « Mes rendez-vous » : deux seulement, conformément à la maquette. */
enum class OngletRendezVous(val libelle: String) {

    /** Rendez-vous confirmés ou en attente, affichés en premier. */
    A_VENIR("À venir"),

    /** Rendez-vous terminés ou annulés, avec leur compte-rendu le cas échéant. */
    PASSES("Passés & Historique"),
}

/** Ordre de tri appliqué à la liste active par l'option de la cloche. */
enum class OrdreTriRendezVous(val libelle: String) {

    /** Du plus récent au plus ancien. */
    RECENT("Plus récents d'abord"),

    /** Du plus ancien au plus récent. */
    ANCIEN("Plus anciens d'abord"),
}

/** États explicites de la liste, affichés pour chaque onglet. */
enum class EtatListeRendezVous {

    /** Chargement initial ou rafraîchissement en cours. */
    CHARGEMENT,

    /** Au moins un rendez-vous est affichable dans l'onglet courant. */
    DONNEES,

    /** L'onglet courant est vide. */
    VIDE,

    /** Le chargement a échoué. */
    ERREUR,
}

/** Premier jour du mois courant, ramené à minuit. */
private fun premierJourDuMoisCourant(): Long =
    Calendar.getInstance(Locale.FRANCE).apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

/** États de la grille de créneaux de la carte de réservation intégrée. */
enum class EtatCreneauxRendezVous {

    /** Aucun jour sélectionné : état initial du calendrier. */
    VIDE,

    /** Chargement des créneaux du jour choisi. */
    CHARGEMENT,

    /** Au moins un créneau est affichable. */
    DONNEES,

    /** Le chargement des créneaux a échoué. */
    ERREUR,
}

/**
 * État de la carte « Prendre un rendez-vous » intégrée en bas de l'onglet « À venir ».
 *
 * @property specialite Spécialité active dans les chips.
 * @property medecin Médecin déduit de la spécialité, utilisé pour demander les créneaux.
 * @property premierJourMois Premier jour du mois affiché par le calendrier.
 * @property jourSelectionne Jour choisi dans la grille, ou 0 avant sélection.
 * @property creneauxMatin Créneaux disponibles ou occupés avant midi.
 * @property creneauxApresMidi Créneaux disponibles ou occupés après midi.
 * @property creneauSelectionne Créneau choisi par le patient.
 * @property etat État de chargement de la grille.
 * @property chargementReservation `true` pendant l'appel de réservation.
 */
data class ReservationUiState(
    val specialite: SpecialiteRendezVous = SpecialiteRendezVous.CARDIOLOGIE,
    val medecin: MedecinCatalogue = MedecinsCatalogue.parSpecialite(SpecialiteRendezVous.CARDIOLOGIE).first(),
    val premierJourMois: Long = premierJourDuMoisCourant(),
    val jourSelectionne: Long = 0L,
    val creneauxMatin: List<TimeSlot> = emptyList(),
    val creneauxApresMidi: List<TimeSlot> = emptyList(),
    val creneauSelectionne: TimeSlot? = null,
    val etat: EtatCreneauxRendezVous = EtatCreneauxRendezVous.VIDE,
    val chargementReservation: Boolean = false,
)

/**
 * État affichable de l'écran « Mes rendez-vous ».
 *
 * @property ongletActif Onglet sélectionné, mémorisé dans le DataStore.
 * @property rendezVous Tous les rendez-vous du patient, avant filtrage d'onglet.
 * @property rendezVousFiltres Rendez-vous de l'onglet courant, déjà triés.
 * @property prochainRendezVous Rendez-vous actif le plus proche, mis en avant dans la carte featured.
 * @property autresAVenir Les autres rendez-vous actifs, affichés en cartes compactes.
 * @property nombreAVenir Compteur dynamique du segmented control.
 * @property delaiProchainRendezVous Libellé du badge « Dans X jours » / « Aujourd'hui ».
 * @property etat État de chargement de l'onglet courant.
 * @property errorMessage Message d'erreur de chargement.
 * @property ordreTri Ordre de tri courant.
 * @property rappelsActifs Valeur persistée des rappels, lue depuis le DataStore.
 * @property reservation État de la carte de réservation intégrée.
 * @property rendezVousACancel Rendez-vous en attente de confirmation d'annulation.
 * @property operationEnCours Annulation en cours : le bouton du dialog est désactivé.
 * @property messageSnackbar Message d'information affiché dans un Snackbar.
 */
data class MyAppointmentsUiState(
    val ongletActif: OngletRendezVous = OngletRendezVous.A_VENIR,
    val rendezVous: List<Appointment> = emptyList(),
    val rendezVousFiltres: List<Appointment> = emptyList(),
    val prochainRendezVous: Appointment? = null,
    val autresAVenir: List<Appointment> = emptyList(),
    val nombreAVenir: Int = 0,
    val delaiProchainRendezVous: String = "",
    val etat: EtatListeRendezVous = EtatListeRendezVous.CHARGEMENT,
    val errorMessage: String? = null,
    val ordreTri: OrdreTriRendezVous = OrdreTriRendezVous.RECENT,
    val rappelsActifs: Boolean = true,
    val reservation: ReservationUiState = ReservationUiState(),
    val rendezVousACancel: Appointment? = null,
    val operationEnCours: Boolean = false,
    val messageSnackbar: String? = null,
)

/**
 * Pilote la consultation, la réservation intégrée, l'annulation et les préférences.
 *
 * Aucune logique métier n'est placée dans les Composables : l'écran n'envoie que des
 * intentions (changement d'onglet, spécialité, jour, créneau, annulation) et affiche cet état.
 *
 * @property repository Source de données sélectionnée par Hilt.
 * @property preferences Préférences DataStore des rendez-vous.
 */
@HiltViewModel
class MyAppointmentsViewModel @Inject constructor(
    private val repository: AppointmentRepository,
    private val preferences: AppointmentPreferences,
) : ViewModel() {

    private val mutableUiState = MutableStateFlow(MyAppointmentsUiState())
    private var observationJob: Job? = null
    private var observationCreneauxJob: Job? = null

    /** État observable par l'écran Compose. */
    val uiState: StateFlow<MyAppointmentsUiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferences.dernierOngletConsulte.collect { cle ->
                val onglet = OngletRendezVous.entries.firstOrNull { it.name == cle }
                    ?: OngletRendezVous.A_VENIR
                mutableUiState.update { it.copy(ongletActif = onglet).avecListesFiltrees() }
            }
        }
        viewModelScope.launch {
            preferences.rappelsActifs.collect { actifs ->
                mutableUiState.update { it.copy(rappelsActifs = actifs) }
            }
        }
        charger()
    }

    /**
     * (Re)charge les rendez-vous depuis le dépôt, y compris via l'option « Actualiser ».
     *
     * @param afficherChargement `false` lors d'un rafraîchissement silencieux, pour ne pas
     * faire clignoter la liste déjà mise à jour localement.
     */
    fun charger(afficherChargement: Boolean = true) {
        observationJob?.cancel()
        if (afficherChargement) {
            mutableUiState.update {
                it.copy(etat = EtatListeRendezVous.CHARGEMENT, errorMessage = null)
            }
        }
        observationJob = viewModelScope.launch {
            repository.observerMesRendezVous(null)
                .catch { cause ->
                    if (cause is CancellationException) throw cause
                    mutableUiState.update {
                        it.copy(
                            etat = EtatListeRendezVous.ERREUR,
                            errorMessage = erreurLisible(cause),
                        )
                    }
                }
                .collect { rendezVous ->
                    mutableUiState.update { etat ->
                        // CORRECTION : les données sont arrivées, on doit recalculer l'état
                        // (VIDE ou DONNEES). Sans ce paramètre, avecListesFiltrees()
                        // préservait CHARGEMENT et l'écran restait bloqué sur le spinner.
                        etat.copy(rendezVous = rendezVous, errorMessage = null)
                            .avecListesFiltrees(conserverEtatTransitoire = false)
                    }
                }
        }
    }

    /**
     * Change d'onglet et persiste le choix pour la prochaine session.
     *
     * @param onglet Onglet sélectionné par l'utilisateur.
     */
    fun selectionnerOnglet(onglet: OngletRendezVous) {
        mutableUiState.update { it.copy(ongletActif = onglet).avecListesFiltrees() }
        viewModelScope.launch { preferences.enregistrerDernierOnglet(onglet.name) }
    }

    /** Inverse l'ordre de tri de la liste courante. */
    fun basculerTri() {
        val suivant = when (mutableUiState.value.ordreTri) {
            OrdreTriRendezVous.RECENT -> OrdreTriRendezVous.ANCIEN
            OrdreTriRendezVous.ANCIEN -> OrdreTriRendezVous.RECENT
        }
        mutableUiState.update { it.copy(ordreTri = suivant).avecListesFiltrees() }
    }

    /** Inverse le réglage des rappels ; le DataStore republie la nouvelle valeur. */
    fun basculerRappels() {
        val newValue = !mutableUiState.value.rappelsActifs
        viewModelScope.launch { preferences.setRappelsActifs(newValue) }
    }

    /**
     * Change la spécialité filtrée et recharge les créneaux du médecin correspondant.
     *
     * @param specialite Spécialité choisie dans les chips.
     */
    fun selectionnerSpecialite(specialite: SpecialiteRendezVous) {
        val medecin = MedecinsCatalogue.parSpecialite(specialite).firstOrNull() ?: return
        mutableUiState.update { etat ->
            etat.copy(
                reservation = etat.reservation.copy(
                    specialite = specialite,
                    medecin = medecin,
                    creneauxMatin = emptyList(),
                    creneauxApresMidi = emptyList(),
                    creneauSelectionne = null,
                    etat = EtatCreneauxRendezVous.VIDE,
                ),
            )
        }
        val jour = mutableUiState.value.reservation.jourSelectionne.takeIf { it > 0L }
            ?: AppointmentFormatters.debutJour(System.currentTimeMillis())
        chargerCreneaux(jour)
    }

    /**
     * Fait défiler le calendrier d'un nombre de mois.
     *
     * @param delta Nombre de mois (négatif pour le mois précédent).
     */
    fun deplacerCalendrier(delta: Int) {
        observationCreneauxJob?.cancel()
        mutableUiState.update { etat ->
            etat.copy(
                reservation = etat.reservation.copy(
                    premierJourMois = AppointmentFormatters.deplacerMois(
                        etat.reservation.premierJourMois,
                        delta,
                    ),
                    jourSelectionne = 0L,
                    creneauxMatin = emptyList(),
                    creneauxApresMidi = emptyList(),
                    creneauSelectionne = null,
                    etat = EtatCreneauxRendezVous.VIDE,
                ),
            )
        }
    }

    /**
     * Sélectionne un jour du calendrier et charge ses créneaux.
     *
     * @param jour Début de journée en millisecondes.
     */
    fun selectionnerJour(jour: Long) {
        mutableUiState.update {
            it.copy(messageSnackbar = "Date sélectionnée : ${AppointmentFormatters.formaterJourMois(jour)}")
        }
        chargerCreneaux(jour)
    }

    /**
     * Sélectionne un créneau disponible dans la grille.
     *
     * @param creneau Créneau choisi par le patient.
     */
    fun selectionnerCreneau(creneau: TimeSlot) {
        if (!creneau.disponible) return
        mutableUiState.update {
            it.copy(reservation = it.reservation.copy(creneauSelectionne = creneau))
        }
    }

    /** Confirme la réservation intégrée et gère notamment le conflit 409. */
    fun confirmerReservation() {
        val reservation = mutableUiState.value.reservation
        val creneau = reservation.creneauSelectionne ?: return
        if (reservation.chargementReservation) return

        mutableUiState.update {
            it.copy(reservation = it.reservation.copy(chargementReservation = true))
        }
        viewModelScope.launch {
            try {
                repository.reserverRendezVous(reservation.medecin.id, creneau.id)
                mutableUiState.update { etat ->
                    etat.copy(
                        reservation = etat.reservation.copy(
                            creneauSelectionne = null,
                            chargementReservation = false,
                            etat = EtatCreneauxRendezVous.VIDE,
                        ),
                        messageSnackbar = "Rendez-vous confirmé pour " +
                            "${AppointmentFormatters.formaterHeureSeule(creneau.heureDebut)} !",
                    )
                }
                charger(afficherChargement = false)
            } catch (annulation: CancellationException) {
                throw annulation
            } catch (conflit: AppointmentError.Conflit) {
                mutableUiState.update {
                    it.copy(
                        reservation = it.reservation.copy(chargementReservation = false),
                        messageSnackbar = conflit.messageUtilisateur,
                    )
                }
                chargerCreneaux(reservation.jourSelectionne)
            } catch (cause: Exception) {
                mutableUiState.update {
                    it.copy(
                        reservation = it.reservation.copy(chargementReservation = false),
                        messageSnackbar = erreurLisible(cause),
                    )
                }
            }
        }
    }

    /**
     * Ouvre le dialog de confirmation d'annulation pour un rendez-vous.
     *
     * @param rendezVous Rendez-vous sur lequel l'utilisateur a appuyé.
     */
    fun demanderAnnulation(rendezVous: Appointment) {
        mutableUiState.update { it.copy(rendezVousACancel = rendezVous, messageSnackbar = null) }
    }

    /** Ferme le dialog de confirmation sans rien modifier. */
    fun fermerDialogAnnulation() {
        mutableUiState.update { it.copy(rendezVousACancel = null, operationEnCours = false) }
    }

    /**
     * Confirme l'annulation : met à jour l'état local immédiatement, puis rafraîchit le serveur.
     *
     * En cas de refus, le message du serveur est affiché dans un Snackbar et la liste
     * n'est PAS modifiée.
     */
    fun confirmerAnnulation() {
        val cible = mutableUiState.value.rendezVousACancel ?: return
        mutableUiState.update { it.copy(operationEnCours = true) }
        viewModelScope.launch {
            try {
                val modifie = repository.annulerRendezVous(cible.id, MOTIF_ANNULATION)
                mutableUiState.update { etat ->
                    etat.copy(
                        rendezVous = etat.rendezVous.map { if (it.id == modifie.id) modifie else it },
                        rendezVousACancel = null,
                        operationEnCours = false,
                        messageSnackbar = "Le rendez-vous a été annulé.",
                    ).avecListesFiltrees()
                }
                charger(afficherChargement = false)
            } catch (annulation: CancellationException) {
                throw annulation
            } catch (cause: Exception) {
                mutableUiState.update {
                    it.copy(
                        operationEnCours = false,
                        rendezVousACancel = null,
                        messageSnackbar = erreurLisible(cause),
                    )
                }
            }
        }
    }

    /**
     * Affiche un message dans le Snackbar, utilisé pour les actions sans écran dédié
     * (relance, compte-rendu, Intent indisponible).
     *
     * @param message Texte à afficher.
     */
    fun afficherMessage(message: String) {
        mutableUiState.update { it.copy(messageSnackbar = message) }
    }

    /** Action du bouton flottant : TODO, le triage IA appartient à une autre feature. */
    fun triageIA() {
        Log.d(TAG, "TODO: déclencher le triage IA (feature hors périmètre US-30..US-35)")
        afficherMessage("Triage IA : connexion à la feature Santé IA à venir.")
    }

    /**
     * Relance une demande en attente.
     *
     * TODO: brancher sur la notification secrétariat lorsque le patient demande la relance.
     *
     * @param rendezVous Demande à relancer.
     */
    fun relancer(rendezVous: Appointment) {
        Log.d(TAG, "TODO: relancer la demande ${rendezVous.id} auprès du secrétariat")
        afficherMessage("Relance envoyée au secrétariat pour ${rendezVous.doctorName}.")
    }

    /**
     * Télécharge un compte-rendu.
     *
     * TODO: brancher sur le service de documents médicaux.
     *
     * @param rendezVous Rendez-vous dont le compte-rendu est demandé.
     */
    fun telechargerCompteRendu(rendezVous: Appointment) {
        Log.d(TAG, "TODO: télécharger le compte-rendu du rendez-vous ${rendezVous.id}")
        afficherMessage("Compte-rendu de ${rendezVous.doctorName} : téléchargement à venir.")
    }

    /** Efface le message du Snackbar une fois celui-ci affiché. */
    fun consommerMessageSnackbar() {
        mutableUiState.update { it.copy(messageSnackbar = null) }
    }

    /** Charge les créneaux du médecin courant pour un jour donné. */
    private fun chargerCreneaux(jour: Long) {
        observationCreneauxJob?.cancel()
        mutableUiState.update { etat ->
            etat.copy(
                reservation = etat.reservation.copy(
                    jourSelectionne = jour,
                    creneauxMatin = emptyList(),
                    creneauxApresMidi = emptyList(),
                    creneauSelectionne = null,
                    etat = EtatCreneauxRendezVous.CHARGEMENT,
                ),
            )
        }
        val medecin = mutableUiState.value.reservation.medecin
        observationCreneauxJob = viewModelScope.launch {
            repository.observerCreneaux(medecin.id, jour)
                .catch { cause ->
                    if (cause is CancellationException) throw cause
                    mutableUiState.update { etat ->
                        etat.copy(
                            reservation = etat.reservation.copy(
                                etat = EtatCreneauxRendezVous.ERREUR,
                            ),
                            messageSnackbar = erreurLisible(cause),
                        )
                    }
                }
                .collect { creneaux ->
                    mutableUiState.update { etat ->
                        val (matin, apresMidi) = creneaux.partition { estAvantMidi(it.heureDebut) }
                        etat.copy(
                            reservation = etat.reservation.copy(
                                creneauxMatin = matin.sortedBy(TimeSlot::heureDebut),
                                creneauxApresMidi = apresMidi.sortedBy(TimeSlot::heureDebut),
                                etat = if (creneaux.isEmpty()) {
                                    EtatCreneauxRendezVous.VIDE
                                } else {
                                    EtatCreneauxRendezVous.DONNEES
                                },
                            ),
                        )
                    }
                }
        }
    }

    /** Indique si un horaire appartient au groupe « Matin ». */
    private fun estAvantMidi(horodatage: Long): Boolean =
        Calendar.getInstance(Locale.FRANCE).apply { timeInMillis = horodatage }
            .get(Calendar.HOUR_OF_DAY) < 12

    /** Convertit une exception en texte affichable. */
    private fun erreurLisible(cause: Throwable): String = when (cause) {
        is AppointmentError -> cause.messageUtilisateur
        else -> cause.message ?: "Impossible de charger les rendez-vous."
    }

    /**
     * Recalcule les listes, le compteur et le libellé du prochain rendez-vous.
     *
     * @param conserverEtatTransitoire `true` par défaut : garde CHARGEMENT ou ERREUR lors d'un
     * changement d'onglet/de tri pendant un chargement. À passer à `false` dès que le dépôt a
     * émis des données, sinon CHARGEMENT restait figé alors que la liste était déjà prête.
     */
    private fun MyAppointmentsUiState.avecListesFiltrees(
        conserverEtatTransitoire: Boolean = true,
    ): MyAppointmentsUiState {
        val actifs = rendezVous
            .filter {
                it.statut == StatutRendezVous.CONFIRME || it.statut == StatutRendezVous.EN_ATTENTE
            }
            .sortedBy(Appointment::heureDebut)
        val historique = rendezVous
            .filter {
                it.statut == StatutRendezVous.TERMINE || it.statut == StatutRendezVous.ANNULE
            }
            .sortedByDescending(Appointment::heureDebut)
        val base = when (ongletActif) {
            OngletRendezVous.A_VENIR -> actifs
            OngletRendezVous.PASSES -> historique
        }
        val filtres = when (ordreTri) {
            OrdreTriRendezVous.RECENT -> base
            OrdreTriRendezVous.ANCIEN -> base.reversed()
        }
        val prochain = actifs.firstOrNull()
        val aujourdHui = AppointmentFormatters.debutJour(System.currentTimeMillis())
        val jours = prochain?.let {
            AppointmentFormatters.nombreJoursEntre(aujourdHui, it.date)
        } ?: 0
        val delai = when {
            prochain == null -> ""
            jours <= 0 -> "Aujourd'hui"
            jours == 1 -> "Demain"
            else -> "Dans $jours jours"
        }
        return copy(
            rendezVousFiltres = filtres,
            prochainRendezVous = prochain,
            autresAVenir = actifs.drop(1),
            nombreAVenir = actifs.size,
            delaiProchainRendezVous = delai,
            etat = when {
                conserverEtatTransitoire &&
                    (etat == EtatListeRendezVous.CHARGEMENT ||
                        etat == EtatListeRendezVous.ERREUR) -> etat
                filtres.isEmpty() -> EtatListeRendezVous.VIDE
                else -> EtatListeRendezVous.DONNEES
            },
        )
    }

    private companion object {
        const val TAG = "MyAppointmentsVM"
        const val MOTIF_ANNULATION = "Annulation demandée par le patient"
    }
}
