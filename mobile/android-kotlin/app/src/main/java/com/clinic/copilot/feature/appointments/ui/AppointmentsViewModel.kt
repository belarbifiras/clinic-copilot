package com.clinic.copilot.feature.appointments.ui

import androidx.lifecycle.ViewModel
import com.clinic.copilot.feature.appointments.domain.DocumentMedical
import com.clinic.copilot.feature.appointments.domain.MomentJour
import com.clinic.copilot.feature.appointments.domain.StatutPrise
import com.clinic.copilot.feature.appointments.domain.TraitementPilulier
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class AppointmentsUiState(
    val ongletActif: Int = 0, // 0 = Traitements, 1 = Historique & Docs
    val momentSelectionne: MomentJour = MomentJour.MIDI,
    val traitements: List<TraitementPilulier> = emptyList(),
    val documents: List<DocumentMedical> = emptyList(),
)

/**
 * ViewModel du Dossier Médical & Pilulier – Membre 4 (Sprint 6, US-30 & US-32).
 */
@HiltViewModel
class AppointmentsViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(AppointmentsUiState())
    val uiState: StateFlow<AppointmentsUiState> = _uiState.asStateFlow()

    init {
        chargerDonnees()
    }

    private fun chargerDonnees() {
        val traites = listOf(
            TraitementPilulier(
                id = "t1",
                nom = "Amoxicilline 500mg",
                posologie = "1 gélule matin et soir au cours du repas",
                creneauHoraire = "08:15",
                momentJour = MomentJour.MATIN,
                statutPrise = StatutPrise.PRIS,
                prescritPar = "Dr. Marc Laurent",
                progressionJours = "Jour 4 / 6",
            ),
            TraitementPilulier(
                id = "t2",
                nom = "Doliprane 1000mg",
                posologie = "Si douleur ou fièvre (max 3/j, espacer de 6h minimum)",
                creneauHoraire = "14:00",
                momentJour = MomentJour.MIDI,
                statutPrise = StatutPrise.A_PRENDRE,
                prescritPar = "Prise ponctuelle si besoin",
                progressionJours = "Si besoin",
            ),
        )

        val docs = listOf(
            DocumentMedical(
                id = "doc-1",
                titre = "Consultation Cardiologie",
                date = "24 Oct 2024",
                medecin = "Dr. Marc Laurent",
                tailleFichier = "PDF 1.2 Mo",
            ),
            DocumentMedical(
                id = "doc-2",
                titre = "Bilan Sanguin & NFS",
                date = "15 Sep 2024",
                medecin = "Laboratoire d'Eylau",
                tailleFichier = "PDF 850 Ko",
            ),
        )

        _uiState.update { it.copy(traitements = traites, documents = docs) }
    }

    fun changerOnglet(index: Int) {
        _uiState.update { it.copy(ongletActif = index) }
    }

    fun selectionnerMoment(moment: MomentJour) {
        _uiState.update { it.copy(momentSelectionne = moment) }
    }

    fun validerPrise(id: String) {
        _uiState.update { state ->
            state.copy(
                traitements = state.traitements.map {
                    if (it.id == id) it.copy(statutPrise = StatutPrise.PRIS, creneauHoraire = "À l'instant") else it
                },
            )
        }
    }
}
