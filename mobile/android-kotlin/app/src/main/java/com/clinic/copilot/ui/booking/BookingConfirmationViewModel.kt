package com.clinic.copilot.ui.booking

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clinic.copilot.data.AppointmentError
import com.clinic.copilot.data.AppointmentRepository
import com.clinic.copilot.domain.Appointment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * État affichable de l'écran de confirmation.
 *
 * @property doctorName Nom du médecin.
 * @property specialite Spécialité du médecin.
 * @property date Jour du rendez-vous.
 * @property heureDebut Début du créneau.
 * @property heureFin Fin du créneau.
 * @property enCours Appel réseau en cours ; le bouton est désactivé.
 * @property conflit Le créneau a été pris entre-temps (HTTP 409).
 * @property rendezVous Rendez-vous créé ou reprogrammé.
 * @property errorMessage Message d'erreur à afficher.
 * @property estReprogrammation `true` pour une reprogrammation (US-34).
 */
data class BookingConfirmationUiState(
    val doctorName: String = "",
    val specialite: String = "",
    val date: Long = 0L,
    val heureDebut: Long = 0L,
    val heureFin: Long = 0L,
    val enCours: Boolean = false,
    val conflit: Boolean = false,
    val rendezVous: Appointment? = null,
    val errorMessage: String? = null,
    val estReprogrammation: Boolean = false,
)

/**
 * Confirme une réservation ou une reprogrammation et gère les erreurs serveur.
 *
 * @property repository Source de données sélectionnée par Hilt.
 * @property savedStateHandle Arguments de navigation décrivant le créneau choisi.
 */
@HiltViewModel
class BookingConfirmationViewModel @Inject constructor(
    private val repository: AppointmentRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val doctorId = savedStateHandle.get<String>(ARG_DOCTOR_ID).orEmpty()
    private val slotId = savedStateHandle.get<String>(ARG_SLOT_ID).orEmpty()
    private val rendezVousId = savedStateHandle.get<String>(ARG_APPOINTMENT_ID).orEmpty()
    private val date = savedStateHandle.get<Long>(ARG_DATE) ?: 0L
    private val heureDebut = savedStateHandle.get<Long>(ARG_HEURE_DEBUT) ?: 0L
    private val heureFin = savedStateHandle.get<Long>(ARG_HEURE_FIN) ?: 0L

    private val mutableUiState = MutableStateFlow(
        BookingConfirmationUiState(
            doctorName = savedStateHandle.get<String>(ARG_DOCTOR_NAME).orEmpty(),
            specialite = savedStateHandle.get<String>(ARG_SPECIALITE).orEmpty(),
            date = date,
            heureDebut = heureDebut,
            heureFin = heureFin,
            estReprogrammation = rendezVousId.isNotBlank(),
        ),
    )

    /** État observable par l'écran Compose. */
    val uiState: StateFlow<BookingConfirmationUiState> = mutableUiState.asStateFlow()

    /**
     * Effectue la réservation ou la reprogrammation, sans double appel possible.
     */
    fun confirmer() {
        val etat = mutableUiState.value
        if (etat.enCours || etat.rendezVous != null || etat.conflit) return

        mutableUiState.update { it.copy(enCours = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val resultat = if (rendezVousId.isBlank()) {
                    repository.reserverRendezVous(doctorId, slotId)
                } else {
                    repository.reprogrammerRendezVous(rendezVousId, slotId)
                }
                // Rafraîchissement serveur : l'ancien créneau doit apparaître libéré après reprogrammation.
                runCatching { repository.observerMesRendezVous(null).first() }
                mutableUiState.update { it.copy(enCours = false, rendezVous = resultat) }
            } catch (annulation: CancellationException) {
                throw annulation
            } catch (cause: AppointmentError.Conflit) {
                // Le 409 déclenche le rechargement des créneaux avant le retour à l'écran précédent.
                runCatching { repository.observerCreneaux(doctorId, date).first() }
                mutableUiState.update {
                    it.copy(
                        enCours = false,
                        conflit = true,
                        errorMessage = cause.messageUtilisateur,
                    )
                }
            } catch (cause: Exception) {
                mutableUiState.update {
                    it.copy(
                        enCours = false,
                        errorMessage = when (cause) {
                            is AppointmentError -> cause.messageUtilisateur
                            else -> cause.message ?: "Impossible de confirmer le rendez-vous."
                        },
                    )
                }
            }
        }
    }

    companion object {

        /** Clé de l'identifiant médecin. */
        const val ARG_DOCTOR_ID = "doctorId"

        /** Clé du nom du médecin. */
        const val ARG_DOCTOR_NAME = "doctorName"

        /** Clé de la spécialité. */
        const val ARG_SPECIALITE = "specialite"

        /** Clé de l'identifiant du créneau. */
        const val ARG_SLOT_ID = "slotId"

        /** Clé du jour du rendez-vous. */
        const val ARG_DATE = "date"

        /** Clé de l'horaire de début. */
        const val ARG_HEURE_DEBUT = "heureDebut"

        /** Clé de l'horaire de fin. */
        const val ARG_HEURE_FIN = "heureFin"

        /** Clé facultative du rendez-vous reprogrammé. */
        const val ARG_APPOINTMENT_ID = "appointmentId"
    }
}
