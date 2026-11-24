package com.clinic.copilot.feature.appointments.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clinic.copilot.core.data.ClinicLocalDatabase
import com.clinic.copilot.core.data.LocalRendezVous
import com.clinic.copilot.core.data.StatutRdv
import com.clinic.copilot.feature.appointments.domain.DocumentMedical
import com.clinic.copilot.feature.appointments.domain.MomentJour
import com.clinic.copilot.feature.appointments.domain.TraitementPilulier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppointmentsUiState(
    val ongletPrincipal: Int = 0, // 0 = Rendez-vous, 1 = Pilulier, 2 = Documents
    val filtreStatutRdv: StatutRdv = StatutRdv.A_VENIR,
    val rendezVous: List<LocalRendezVous> = emptyList(),
    val filteredRendezVous: List<LocalRendezVous> = emptyList(),
    val traitements: List<TraitementPilulier> = emptyList(),
    val documents: List<DocumentMedical> = emptyList(),
    val momentSelectionne: MomentJour = MomentJour.MIDI,
    val rdvPourAvis: LocalRendezVous? = null,
    val rdvPourAnnulation: LocalRendezVous? = null,
    val messageNotification: String? = null,
)

/**
 * ViewModel complet du module Rendez-vous & Pilulier (US-30, US-31, US-32, US-33, US-80, US-90, US-91).
 */
@HiltViewModel
class AppointmentsViewModel @Inject constructor(
    private val database: ClinicLocalDatabase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppointmentsUiState())
    val uiState: StateFlow<AppointmentsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                database.rendezVous,
                database.traitements,
                database.documents,
            ) { rdvs, traites, docs ->
                Triple(rdvs, traites, docs)
            }.collect { (rdvs, traites, docs) ->
                _uiState.update { state ->
                    val filtered = rdvs.filter { it.statut == state.filtreStatutRdv }
                    state.copy(
                        rendezVous = rdvs,
                        filteredRendezVous = filtered,
                        traitements = traites,
                        documents = docs,
                    )
                }
            }
        }
    }

    fun changerOngletPrincipal(index: Int) {
        _uiState.update { it.copy(ongletPrincipal = index) }
    }

    fun changerFiltreStatutRdv(statut: StatutRdv) {
        _uiState.update { state ->
            state.copy(
                filtreStatutRdv = statut,
                filteredRendezVous = state.rendezVous.filter { it.statut == statut },
            )
        }
    }

    fun selectionnerMoment(moment: MomentJour) {
        _uiState.update { it.copy(momentSelectionne = moment) }
    }

    fun validerPrise(id: String) {
        database.validerPriseMedicament(id)
    }

    fun demanderAnnulation(rdv: LocalRendezVous) {
        _uiState.update { it.copy(rdvPourAnnulation = rdv) }
    }

    fun fermerDialogueAnnulation() {
        _uiState.update { it.copy(rdvPourAnnulation = null) }
    }

    fun confirmerAnnulation(rdv: LocalRendezVous) {
        database.annulerRdv(rdv.id)
        _uiState.update {
            it.copy(
                rdvPourAnnulation = null,
                messageNotification = "Le rendez-vous avec ${rdv.nomMedecin} a bien été annulé.",
            )
        }
    }

    fun ouvrirDialogueAvis(rdv: LocalRendezVous) {
        _uiState.update { it.copy(rdvPourAvis = rdv) }
    }

    fun fermerDialogueAvis() {
        _uiState.update { it.copy(rdvPourAvis = null) }
    }

    fun soumettreAvis(rdv: LocalRendezVous, note: Int, commentaire: String) {
        database.noterRdv(rdv.id, note, commentaire)
        _uiState.update {
            it.copy(
                rdvPourAvis = null,
                messageNotification = "Merci pour votre avis ($note/5) sur ${rdv.nomMedecin} !",
            )
        }
    }

    fun effacerMessageNotification() {
        _uiState.update { it.copy(messageNotification = null) }
    }
}
