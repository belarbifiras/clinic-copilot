package com.clinic.copilot.feature.auth.data

import com.clinic.copilot.feature.auth.domain.AuthRepository
import com.clinic.copilot.feature.auth.domain.AuthResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay

/**
 * Dépôt de démonstration en mémoire : simule un délai réseau de 600 ms
 * et accepte n'importe quel email avec un mot de passe d'au moins 4 caractères.
 *
 * Remplacer par AuthRepositoryImpl (Retrofit → identity-profile-service) pour la prod.
 */
@Singleton
class FakeAuthRepository @Inject constructor() : AuthRepository {

    private val comptes = mutableMapOf(
        "patient@demo.fr" to "demo1234",
        "sophie.laurent@sante.fr" to "password",
    )

    override suspend fun seConnecter(email: String, motDePasse: String): AuthResult {
        delay(DELAI_MS)
        val emailNorm = email.trim().lowercase()
        return when {
            emailNorm.isBlank() || motDePasse.isBlank() ->
                AuthResult.Erreur("Veuillez remplir tous les champs.")
            comptes[emailNorm] == motDePasse ->
                AuthResult.Succes(userId = "patient-demo")
            comptes.containsKey(emailNorm) ->
                AuthResult.Erreur("Mot de passe incorrect.")
            else ->
                // En mode demo : on accepte tout email valide avec mdp ≥ 4 chars
                if (motDePasse.length >= 4 && emailNorm.contains("@")) {
                    comptes[emailNorm] = motDePasse
                    AuthResult.Succes(userId = emailNorm.replace("@", "_"))
                } else {
                    AuthResult.Erreur("Identifiants invalides.")
                }
        }
    }

    override suspend fun sInscrire(
        prenom: String,
        nom: String,
        email: String,
        motDePasse: String,
    ): AuthResult {
        delay(DELAI_MS)
        val emailNorm = email.trim().lowercase()
        return when {
            prenom.isBlank() || nom.isBlank() ->
                AuthResult.Erreur("Prénom et nom obligatoires.")
            emailNorm.isBlank() || !emailNorm.contains("@") ->
                AuthResult.Erreur("Adresse e-mail invalide.")
            motDePasse.length < 6 ->
                AuthResult.Erreur("Le mot de passe doit comporter au moins 6 caractères.")
            comptes.containsKey(emailNorm) ->
                AuthResult.Erreur("Un compte existe déjà avec cet e-mail.")
            else -> {
                comptes[emailNorm] = motDePasse
                AuthResult.Succes(userId = emailNorm.replace("@", "_"))
            }
        }
    }

    private companion object {
        const val DELAI_MS = 600L
    }
}
