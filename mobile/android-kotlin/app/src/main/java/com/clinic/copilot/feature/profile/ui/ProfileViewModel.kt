package com.clinic.copilot.feature.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clinic.copilot.feature.profile.domain.PatientProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val profile: PatientProfile = PatientProfile(),
    val estDeconnecte: Boolean = false,
)

/**
 * ViewModel du profil patient – Membre 2 (US-13).
 */
@HiltViewModel
class ProfileViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun toggleConsentementTriageIA(actif: Boolean) {
        _uiState.update { it.copy(profile = it.profile.copy(consentementTriageIA = actif)) }
    }

    fun toggleRappelsRdv(actif: Boolean) {
        _uiState.update { it.copy(profile = it.profile.copy(rappelsRdvActifs = actif)) }
    }

    fun seDeconnecter() {
        viewModelScope.launch {
            _uiState.update { it.copy(estDeconnecte = true) }
        }
    }
}
