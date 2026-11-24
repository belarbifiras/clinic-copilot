package com.clinic.copilot.feature.doctors.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clinic.copilot.core.data.ClinicLocalDatabase
import com.clinic.copilot.feature.doctors.domain.Doctor
import com.clinic.copilot.feature.doctors.domain.FiltreMedecin
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DoctorsUiState(
    val doctors: List<Doctor> = emptyList(),
    val filteredDoctors: List<Doctor> = emptyList(),
    val searchQuery: String = "",
    val activeFilter: FiltreMedecin = FiltreMedecin.TOUS,
    val showAiBanner: Boolean = true,
    val isMapView: Boolean = false,
    val selectedDoctorForDetail: Doctor? = null,
    val selectedDoctorForBooking: Doctor? = null,
    val bookingSuccessMessage: String? = null,
    val doctorEpingleCarte: Doctor? = null,
)

/**
 * ViewModel de l'annuaire des médecins – Connecté à [ClinicLocalDatabase] (US-20, US-21, US-22, US-31).
 */
@HiltViewModel
class DoctorsViewModel @Inject constructor(
    private val database: ClinicLocalDatabase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DoctorsUiState())
    val uiState: StateFlow<DoctorsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            database.doctors.collect { liste ->
                _uiState.update { state ->
                    val filtered = filtrer(liste, state.searchQuery, state.activeFilter)
                    state.copy(
                        doctors = liste,
                        filteredDoctors = filtered,
                        doctorEpingleCarte = state.doctorEpingleCarte ?: filtered.firstOrNull(),
                    )
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { state ->
            val filtered = filtrer(state.doctors, query, state.activeFilter)
            state.copy(
                searchQuery = query,
                filteredDoctors = filtered,
                doctorEpingleCarte = filtered.firstOrNull(),
            )
        }
    }

    fun filtrerParSpecialite(specialite: String?) {
        if (specialite.isNullOrBlank()) return
        onSearchQueryChange(specialite)
    }

    fun onFilterSelect(filter: FiltreMedecin) {
        _uiState.update { state ->
            val filtered = filtrer(state.doctors, state.searchQuery, filter)
            state.copy(
                activeFilter = filter,
                filteredDoctors = filtered,
                doctorEpingleCarte = filtered.firstOrNull(),
            )
        }
    }

    fun toggleMapView() {
        _uiState.update { it.copy(isMapView = !it.isMapView) }
    }

    fun selectionnerMedecinCarte(doctor: Doctor) {
        _uiState.update { it.copy(doctorEpingleCarte = doctor) }
    }

    fun ouvrirFicheMedecin(doctor: Doctor) {
        _uiState.update { it.copy(selectedDoctorForDetail = doctor) }
    }

    fun fermerFicheMedecin() {
        _uiState.update { it.copy(selectedDoctorForDetail = null) }
    }

    fun ouvrirReservation(doctor: Doctor) {
        _uiState.update {
            it.copy(
                selectedDoctorForBooking = doctor,
                selectedDoctorForDetail = null, // Ferme la fiche si ouverte
            )
        }
    }

    fun fermerReservation() {
        _uiState.update { it.copy(selectedDoctorForBooking = null) }
    }

    fun confirmerReservation(
        doctor: Doctor,
        dateTexte: String,
        heure: String,
        motif: String,
        teleconsultation: Boolean = false,
    ) {
        database.reserverRdv(
            doctor = doctor,
            dateTexte = dateTexte,
            heure = heure,
            motif = motif,
            teleconsultation = teleconsultation,
        )
        _uiState.update {
            it.copy(
                selectedDoctorForBooking = null,
                bookingSuccessMessage = "Rendez-vous confirmé avec ${doctor.nomComplet} le $dateTexte à $heure !",
            )
        }
    }

    fun effacerMessageSucces() {
        _uiState.update { it.copy(bookingSuccessMessage = null) }
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
