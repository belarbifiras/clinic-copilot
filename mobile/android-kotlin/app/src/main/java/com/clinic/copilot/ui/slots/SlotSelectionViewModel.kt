package com.clinic.copilot.ui.slots

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clinic.copilot.data.AppointmentError
import com.clinic.copilot.data.AppointmentRepository
import com.clinic.copilot.domain.TimeSlot
import com.clinic.copilot.ui.theme.AppointmentFormatters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** États explicites de l'écran de sélection de créneau. */
enum class EtatSlots {

    /** Chargement des créneaux en cours. */
    CHARGEMENT,

    /** Au moins un créneau est affichable. */
    DONNEES,

    /** Aucun créneau disponible pour ce jour. */
    VIDE,

    /** Le chargement a échoué. */
    ERREUR,
}

/**
 * État affichable de l'écran « Choisir un créneau ».
 *
 * @property doctorId Identifiant du médecin concerné.
 * @property doctorName Nom affiché du médecin.
 * @property specialite Spécialité affichée sous le nom.
 * @property dates Sept prochains jours affichés dans la rangée de chips.
 * @property dateSelectionnee Jour courant, en millisecondes.
 * @property creneaux Créneaux du jour sélectionné.
 * @property creneauSelectionne Créneau choisi par l'utilisateur, ou `null`.
 * @property etat État de chargement.
 * @property errorMessage Message d'erreur éventuel.
 * @property rendezVousId Identifiant du rendez-vous à reprogrammer, ou vide.
 * @property estReprogrammation `true` lorsque l'écran sert à reprogrammer un rendez-vous.
 */
data class SlotSelectionUiState(
    val doctorId: String = "",
    val doctorName: String = "",
    val specialite: String = "",
    val dates: List<Long> = emptyList(),
    val dateSelectionnee: Long = 0L,
    val creneaux: List<TimeSlot> = emptyList(),
    val creneauSelectionne: TimeSlot? = null,
    val etat: EtatSlots = EtatSlots.CHARGEMENT,
    val errorMessage: String? = null,
    val rendezVousId: String = "",
    val estReprogrammation: Boolean = false,
) {

    /** Le bouton « Continuer » n'est actif que si un créneau disponible est choisi. */
    val peutContinuer: Boolean
        get() = creneauSelectionne?.disponible == true
}

/**
 * Charge les créneaux du médecin et pilote la sélection de date et d'horaire.
 *
 * @property repository Source de données sélectionnée par Hilt.
 * @property savedStateHandle Arguments de navigation (médecin et rendez-vous à reprogrammer).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SlotSelectionViewModel @Inject constructor(
    private val repository: AppointmentRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val dates = AppointmentFormatters.joursSuivants()
    private val doctorId = savedStateHandle.get<String>(ARG_DOCTOR_ID)
        ?.takeIf { it.isNotBlank() }
        ?: DEFAULT_DOCTOR_ID
    private val doctorName = savedStateHandle.get<String>(ARG_DOCTOR_NAME)
        ?.takeIf { it.isNotBlank() }
        ?: AppointmentFormatters.nomMedecin(doctorId)
    private val specialite = savedStateHandle.get<String>(ARG_SPECIALITE)
        ?.takeIf { it.isNotBlank() }
        ?: AppointmentFormatters.specialiteMedecin(doctorId)
    private val rendezVousId = savedStateHandle.get<String>(ARG_APPOINTMENT_ID).orEmpty()
    private val dateInitiale = savedStateHandle.get<Long>(ARG_DATE)
        ?.takeIf { it != 0L }
        ?.let { valeur -> dates.firstOrNull { it == valeur } }
        ?: dates.first()

    private val dateEtRafraichissement = MutableStateFlow(DateEtToken(dateInitiale, 0))
    private val mutableUiState = MutableStateFlow(
        SlotSelectionUiState(
            doctorId = doctorId,
            doctorName = doctorName,
            specialite = specialite,
            dates = dates,
            dateSelectionnee = dateInitiale,
            rendezVousId = rendezVousId,
            estReprogrammation = rendezVousId.isNotBlank(),
        ),
    )

    /** État observable par l'écran Compose. */
    val uiState: StateFlow<SlotSelectionUiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            dateEtRafraichissement
                .flatMapLatest { (date, _) -> repository.observerCreneaux(doctorId, date) }
                .catch { cause ->
                    mutableUiState.update {
                        it.copy(etat = EtatSlots.ERREUR, errorMessage = erreurLisible(cause))
                    }
                }
                .collect { creneaux ->
                    mutableUiState.update { etat ->
                        etat.copy(
                            creneaux = creneaux,
                            dateSelectionnee = dateEtRafraichissement.value.date,
                            etat = if (creneaux.isEmpty()) EtatSlots.VIDE else EtatSlots.DONNEES,
                            errorMessage = null,
                            creneauSelectionne = null,
                        )
                    }
                }
        }
    }

    /**
     * Change le jour affiché et recharge les créneaux correspondants.
     *
     * @param date Début de journée en millisecondes.
     */
    fun selectionnerDate(date: Long) {
        mutableUiState.update {
            it.copy(dateSelectionnee = date, etat = EtatSlots.CHARGEMENT, creneauSelectionne = null)
        }
        dateEtRafraichissement.update { it.copy(date = date, token = it.token + 1) }
    }

    /**
     * Sélectionne un créneau disponible.
     *
     * @param creneau Créneau choisi par l'utilisateur.
     */
    fun selectionnerCreneau(creneau: TimeSlot) {
        if (!creneau.disponible) return
        mutableUiState.update { it.copy(creneauSelectionne = creneau) }
    }

    /** Relance le chargement des créneaux, notamment après un conflit 409. */
    fun actualiser() {
        mutableUiState.update { it.copy(etat = EtatSlots.CHARGEMENT, errorMessage = null) }
        dateEtRafraichissement.update { it.copy(token = it.token + 1) }
    }

    /** Convertit une exception en texte affichable. */
    private fun erreurLisible(cause: Throwable): String = when (cause) {
        is AppointmentError -> cause.messageUtilisateur
        else -> cause.message ?: "Impossible de charger les créneaux."
    }

    private data class DateEtToken(val date: Long, val token: Int)

    companion object {

        /** Clé de l'identifiant médecin transmis par la navigation. */
        const val ARG_DOCTOR_ID = "doctorId"

        /** Clé du nom du médecin transmis par la navigation. */
        const val ARG_DOCTOR_NAME = "doctorName"

        /** Clé de la spécialité transmise par la navigation. */
        const val ARG_SPECIALITE = "specialite"

        /** Clé facultative du rendez-vous à reprogrammer. */
        const val ARG_APPOINTMENT_ID = "appointmentId"

        /** Clé facultative du jour à présenter, utilisée après un conflit 409. */
        const val ARG_DATE = "date"

        private const val DEFAULT_DOCTOR_ID = "doctor-1"
    }
}
