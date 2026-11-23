package com.clinic.copilot.feature.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clinic.copilot.feature.auth.domain.AuthRepository
import com.clinic.copilot.feature.auth.domain.AuthResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** État partagé des deux écrans d'authentification (connexion + inscription). */
data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    /** Non-nul dès que l'authentification réussit ; contient le userId. */
    val utilisateurConnecteId: String? = null,
)

/**
 * ViewModel commun pour LoginScreen et RegisterScreen.
 *
 * @property repository Source d'authentification (Fake en démo, Retrofit en prod).
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /**
     * Lance la connexion avec email + mot de passe.
     *
     * @param email     E-mail saisi.
     * @param motDePasse Mot de passe saisi.
     */
    fun seConnecter(email: String, motDePasse: String) {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = repository.seConnecter(email, motDePasse)
            _uiState.update {
                when (result) {
                    is AuthResult.Succes -> it.copy(isLoading = false, utilisateurConnecteId = result.userId)
                    is AuthResult.Erreur -> it.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    /**
     * Lance la création d'un compte patient.
     *
     * @param prenom     Prénom saisi.
     * @param nom        Nom saisi.
     * @param email      E-mail saisi.
     * @param motDePasse Mot de passe saisi.
     */
    fun sInscrire(prenom: String, nom: String, email: String, motDePasse: String) {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = repository.sInscrire(prenom, nom, email, motDePasse)
            _uiState.update {
                when (result) {
                    is AuthResult.Succes -> it.copy(isLoading = false, utilisateurConnecteId = result.userId)
                    is AuthResult.Erreur -> it.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    /** Efface le message d'erreur après que l'utilisateur en a pris connaissance. */
    fun effacerErreur() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
