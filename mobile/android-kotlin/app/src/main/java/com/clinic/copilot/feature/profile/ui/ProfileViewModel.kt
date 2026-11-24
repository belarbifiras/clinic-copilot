package com.clinic.copilot.feature.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clinic.copilot.core.data.ClinicLocalDatabase
import com.clinic.copilot.feature.profile.domain.PatientProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val profile: PatientProfile = PatientProfile(),
    val estDeconnecte: Boolean = false,
)

/**
 * ViewModel du profil patient – Connecté à [ClinicLocalDatabase] (US-13).
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val database: ClinicLocalDatabase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            database.currentUser.collect { user ->
                if (user != null) {
                    _uiState.update {
                        it.copy(
                            profile = PatientProfile(
                                nomComplet = "${user.prenom} ${user.nom}",
                                email = user.email,
                                numeroSecuMasque = user.numeroSecuMasque,
                                medecinTraitant = user.medecinTraitant,
                                medecinTraitantNom = user.medecinTraitant,
                                dateNaissance = user.dateNaissance,
                                groupeSanguin = "O+",
                                allergies = user.allergies,
                                antecedents = user.antecedents,
                                consentementTriageIA = user.consentementTriageIA,
                                rappelsRdvActifs = user.rappelsRdvActifs,
                            ),
                            estDeconnecte = false,
                        )
                    }
                }
            }
        }
    }

    fun toggleConsentementTriageIA(actif: Boolean) {
        _uiState.update { it.copy(profile = it.profile.copy(consentementTriageIA = actif)) }
    }

    fun toggleRappelsRdv(actif: Boolean) {
        _uiState.update { it.copy(profile = it.profile.copy(rappelsRdvActifs = actif)) }
    }

    fun seDeconnecter() {
        database.seDeconnecter()
        _uiState.update { it.copy(estDeconnecte = true) }
    }
}
