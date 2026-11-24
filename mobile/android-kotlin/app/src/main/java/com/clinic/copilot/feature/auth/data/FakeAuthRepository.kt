package com.clinic.copilot.feature.auth.data

import com.clinic.copilot.core.data.ClinicLocalDatabase
import com.clinic.copilot.core.session.DemoTokenProvider
import com.clinic.copilot.core.session.TokenProvider
import com.clinic.copilot.feature.auth.domain.AuthRepository
import com.clinic.copilot.feature.auth.domain.AuthResult
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dépôt d'authentification connecté à la base locale unifiée [ClinicLocalDatabase].
 * Valide rigoureusement les identifiants et enregistre la session courante.
 */
@Singleton
class FakeAuthRepository @Inject constructor(
    private val database: ClinicLocalDatabase,
    private val tokenProvider: TokenProvider,
) : AuthRepository {

    override suspend fun seConnecter(email: String, motDePasse: String): AuthResult {
        delay(DELAI_MS)
        val resultat = database.seConnecter(email, motDePasse)
        if (resultat is AuthResult.Succes) {
            (tokenProvider as? DemoTokenProvider)?.enregistrerToken("fake-jwt-${resultat.userId}")
        }
        return resultat
    }

    override suspend fun sInscrire(
        prenom: String,
        nom: String,
        email: String,
        motDePasse: String,
    ): AuthResult {
        delay(DELAI_MS)
        val resultat = database.sInscrire(prenom, nom, email, motDePasse)
        if (resultat is AuthResult.Succes) {
            (tokenProvider as? DemoTokenProvider)?.enregistrerToken("fake-jwt-${resultat.userId}")
        }
        return resultat
    }

    private companion object {
        const val DELAI_MS = 350L
    }
}
