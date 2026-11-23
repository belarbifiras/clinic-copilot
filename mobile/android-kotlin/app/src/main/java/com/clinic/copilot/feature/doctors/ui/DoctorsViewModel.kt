package com.clinic.copilot.feature.doctors.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clinic.copilot.feature.doctors.domain.Doctor
import com.clinic.copilot.feature.doctors.domain.FiltreMedecin
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DoctorsUiState(
    val doctors: List<Doctor> = emptyList(),
    val filteredDoctors: List<Doctor> = emptyList(),
    val searchQuery: String = "",
    val activeFilter: FiltreMedecin = FiltreMedecin.TOUS,
    val showAiBanner: Boolean = true,
)

/**
 * ViewModel de l'annuaire des médecins – Membre 3 (Sprint 6, US-20).
 */
@HiltViewModel
class DoctorsViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(DoctorsUiState())
    val uiState: StateFlow<DoctorsUiState> = _uiState.asStateFlow()

    init {
        chargerMedecins()
    }

    private fun chargerMedecins() {
        val liste = listOf(
            Doctor(
                id = "doc-1",
                nomComplet = "Dr. Éléonore Garnier",
                specialite = "Médecin Généraliste",
                secteur = "Secteur 1",
                note = 4.9,
                nombreAvis = 132,
                adresse = "Paris 15e",
                distanceKm = 1.2,
                disponibleAujourdhui = true,
                teleconsultation = true,
                prochainsCreneaux = listOf("14:30", "15:00", "16:30"),
            ),
            Doctor(
                id = "doc-2",
                nomComplet = "Dr. Marc Laurent",
                specialite = "Cardiologie préventive",
                secteur = "Secteur 1",
                note = 4.8,
                nombreAvis = 98,
                adresse = "Paris 14e",
                distanceKm = 2.4,
                disponibleAujourdhui = false,
                teleconsultation = true,
                prochainsCreneaux = listOf("Demain 14:30"),
            ),
            Doctor(
                id = "doc-3",
                nomComplet = "Dr. Thomas Bernard",
                specialite = "Neurologie",
                secteur = "Secteur 2",
                note = 4.9,
                nombreAvis = 85,
                adresse = "Paris 6e",
                distanceKm = 3.1,
                disponibleAujourdhui = true,
                teleconsultation = false,
                prochainsCreneaux = listOf("11:00", "15:30"),
            ),
            Doctor(
                id = "doc-4",
                nomComplet = "Dr. Nadia Benali",
                specialite = "Dermatologie",
                secteur = "Secteur 1",
                note = 4.7,
                nombreAvis = 64,
                adresse = "Boulogne-Billancourt",
                distanceKm = 4.5,
                disponibleAujourdhui = false,
                teleconsultation = true,
                prochainsCreneaux = listOf("Jeudi 09:30"),
            ),
        )
        _uiState.update { it.copy(doctors = liste, filteredDoctors = liste) }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { state ->
            val filtered = filtrer(state.doctors, query, state.activeFilter)
            state.copy(searchQuery = query, filteredDoctors = filtered)
        }
    }

    fun onFilterSelect(filter: FiltreMedecin) {
        _uiState.update { state ->
            val filtered = filtrer(state.doctors, state.searchQuery, filter)
            state.copy(activeFilter = filter, filteredDoctors = filtered)
        }
    }

    fun dismissAiBanner() {
        _uiState.update { it.copy(showAiBanner = false) }
    }

    private fun filtrer(
        liste: List<Doctor>,
        query: String,
        filtre: FiltreMedecin,
    ): List<Doctor> {
        val q = query.trim().lowercase()
        return liste.filter { doc ->
            val matchQuery = q.isEmpty() ||
                doc.nomComplet.lowercase().contains(q) ||
                doc.specialite.lowercase().contains(q) ||
                doc.adresse.lowercase().contains(q)

            val matchFilter = when (filtre) {
                FiltreMedecin.TOUS -> true
                FiltreMedecin.DISPO_AUJOURDHUI -> doc.disponibleAujourdhui
                FiltreMedecin.TELECONSULTATION -> doc.teleconsultation
            }
            matchQuery && matchFilter
        }
    }
}
