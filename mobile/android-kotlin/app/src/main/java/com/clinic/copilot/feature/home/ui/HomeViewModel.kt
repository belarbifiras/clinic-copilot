package com.clinic.copilot.feature.home.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clinic.copilot.feature.home.domain.HomeData
import com.clinic.copilot.feature.home.domain.MedicamentJour
import com.clinic.copilot.feature.home.domain.ProchainRdv
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val homeData: HomeData? = null,
    val naviguerVersTriage: Boolean = false,
)

/**
 * ViewModel de l'écran d'accueil – Membre 2.
 *
 * Données fictives pré-chargées pour la démo ; à brancher sur
 * patient-profile-service + appointment-service en production.
 */
@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        charger()
    }

    private fun charger() {
        viewModelScope.launch {
            delay(400)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    homeData = HomeData(
                        prenomPatient = "Thomas",
                        prochainRdv = ProchainRdv(
                            nomMedecin = "Dr. Marc Laurent",
                            specialite = "Cardiologue préventif",
                            estConfirme = true,
                            dateTexte = "Demain, 24 Octobre à 14h30",
                            heure = "14h30",
                            lieu = "Clinique Pasteur, Bâtiment B – Stage 7",
                        ),
                        medicamentsJour = listOf(
                            MedicamentJour("Amoxicilline 500mg", "Matin · 08:00 (Matin après le repas)", pris = false),
                            MedicamentJour("Doliprane 1000mg", "Matin · 08:00 (si douleur ou fièvre)", pris = false),
                        ),
                        triageSauvegarde = true,
                    ),
                )
            }
        }
    }

    fun demanderTriage() {
        _uiState.update { it.copy(naviguerVersTriage = true) }
    }

    fun triageNavigueDone() {
        _uiState.update { it.copy(naviguerVersTriage = false) }
    }

    fun validerPriseMedicament(nom: String) {
        _uiState.update { state ->
            val data = state.homeData ?: return@update state
            state.copy(
                homeData = data.copy(
                    medicamentsJour = data.medicamentsJour.map {
                        if (it.nom == nom) it.copy(pris = true) else it
                    },
                ),
            )
        }
    }
}
