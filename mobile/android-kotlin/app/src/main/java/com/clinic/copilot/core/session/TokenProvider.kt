package com.clinic.copilot.core.session

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Source unique du jeton JWT utilisé par les appels réseau de la messagerie.
 *
 * Cette abstraction permet à la feature d'authentification (autre US) de fournir
 * plus tard sa propre implémentation sans modifier le client Retrofit.
 */
interface TokenProvider {

    /**
     * Retourne le JWT courant, ou `null` si l'utilisateur n'est pas connecté.
     */
    fun obtenirToken(): String?

    /**
     * Retourne l'identifiant de l'utilisateur courant, utilisé pour choisir
     * le sens des bulles et identifier le contact dans une conversation.
     */
    fun obtenirUtilisateurId(): String
}

/**
 * Implémentation de démonstration, conservée en mémoire le temps que la feature
 * auth branchée son propre stockage sécurisé.
 */
@Singleton
class DemoTokenProvider @Inject constructor() : TokenProvider {

    @Volatile
    private var token: String? = null

    /**
     * Enregistre le JWT à réutiliser par l'intercepteur OkHttp.
     *
     * @param nouveauToken Jeton brut JWT ; une valeur vide efface le jeton.
     */
    fun enregistrerToken(nouveauToken: String?) {
        token = nouveauToken?.takeIf { it.isNotBlank() }
    }

    override fun obtenirToken(): String? = token

    override fun obtenirUtilisateurId(): String = UTILISATEUR_DEMO

    private companion object {
        const val UTILISATEUR_DEMO = "patient-demo"
    }
}
