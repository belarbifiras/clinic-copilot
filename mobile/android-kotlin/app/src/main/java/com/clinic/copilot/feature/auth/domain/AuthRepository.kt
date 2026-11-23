package com.clinic.copilot.feature.auth.domain

/**
 * Contrat d'authentification : connexion et inscription patient.
 *
 * L'implémentation réelle (AuthRepositoryImpl) appelle l'identity-profile-service ;
 * le FakeAuthRepository est utilisé en mode démo sans serveur.
 */
interface AuthRepository {

    /**
     * Tente une connexion avec email et mot de passe.
     *
     * @param email Adresse e-mail saisie.
     * @param motDePasse Mot de passe saisi.
     * @return [AuthResult.Succes] avec le userId, ou [AuthResult.Erreur] avec un message.
     */
    suspend fun seConnecter(email: String, motDePasse: String): AuthResult

    /**
     * Crée un nouveau compte patient.
     *
     * @param prenom Prénom du patient.
     * @param nom    Nom du patient.
     * @param email  Adresse e-mail.
     * @param motDePasse Mot de passe choisi.
     * @return [AuthResult.Succes] ou [AuthResult.Erreur].
     */
    suspend fun sInscrire(
        prenom: String,
        nom: String,
        email: String,
        motDePasse: String,
    ): AuthResult
}

/** Résultat d'une opération d'authentification. */
sealed interface AuthResult {
    /** Authentification réussie. @property userId Identifiant retourné par le serveur. */
    data class Succes(val userId: String) : AuthResult

    /** Authentification échouée. @property message Raison affichable à l'utilisateur. */
    data class Erreur(val message: String) : AuthResult
}
