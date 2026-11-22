package com.clinic.copilot.data

import com.clinic.copilot.data.remote.dto.ErreurResponse
import com.squareup.moshi.Moshi
import retrofit2.HttpException
import java.io.IOException

/**
 * Erreurs métier claires, exposées aux ViewModels à la place des exceptions Retrofit.
 *
 * @property messageUtilisateur Texte prêt à être affiché dans l'état d'erreur de l'écran.
 * @property origine Exception d'origine, conservée pour le diagnostic.
 */
sealed class ChatError(
    val messageUtilisateur: String,
    val origine: Throwable? = null,
) : Exception(messageUtilisateur) {

    /** JWT absent, expiré ou invalide (HTTP 401). */
    object NonAuthentifie : ChatError("Session expirée, veuillez vous reconnecter.")

    /** L'utilisateur n'appartient pas à la conversation (HTTP 403). */
    object AccesInterdit : ChatError("Accès interdit à cette conversation.")

    /** Conversation ou message introuvable (HTTP 404). */
    object Introuvable : ChatError("Conversation introuvable.")

    /** Erreur interne du service (HTTP 5xx). */
    data class Serveur(val code: Int, val detail: String? = null) : ChatError(
        detail?.takeIf { it.isNotBlank() }?.let { "Erreur serveur ($code) : $it" }
            ?: "Erreur serveur ($code), veuillez réessayer.",
    )

    /** Réseau indisponible ou requête interrompue. */
    object Reseau : ChatError("Connexion indisponible, vérifiez le réseau.")

    /** Cas non prévu, conservé avec son exception d'origine. */
    data class Inconnu(val erreur: Throwable) : ChatError(
        "Une erreur inattendue est survenue.",
        erreur,
    )
}

/**
 * Convertit une exception technique en [ChatError] lisible par l'interface.
 *
 * @param moshi Parseur utilisé pour lire le champ `message` du JSON d'erreur du serveur.
 */
internal fun Throwable.commeChatError(moshi: Moshi): ChatError = when (this) {
    is ChatError -> this
    is HttpException -> when (code()) {
        401 -> ChatError.NonAuthentifie
        403 -> ChatError.AccesInterdit
        404 -> ChatError.Introuvable
        in 500..599 -> ChatError.Serveur(code(), lireMessageServeur(moshi))
        else -> ChatError.Inconnu(this)
    }
    is IOException -> ChatError.Reseau
    else -> ChatError.Inconnu(this)
}

/** Lit `{"message":"..."}` dans le corps d'erreur, sans jamais faire échouer la conversion. */
private fun HttpException.lireMessageServeur(moshi: Moshi): String? = try {
    response()?.errorBody()?.string()?.let { corps ->
        moshi.adapter(ErreurResponse::class.java).fromJson(corps)?.message
    }
} catch (_: Exception) {
    null
}
