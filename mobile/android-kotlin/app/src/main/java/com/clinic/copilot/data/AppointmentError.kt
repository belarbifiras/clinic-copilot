package com.clinic.copilot.data

import com.clinic.copilot.data.remote.dto.ErreurResponse
import com.squareup.moshi.Moshi
import retrofit2.HttpException
import java.io.IOException

/**
 * Erreurs métier claires, exposées aux ViewModels à la place des exceptions Retrofit.
 *
 * @property messageUtilisateur Texte prêt à être affiché dans l'interface.
 * @property origine Exception d'origine, conservée pour le diagnostic.
 */
sealed class AppointmentError(
    val messageUtilisateur: String,
    val origine: Throwable? = null,
) : Exception(messageUtilisateur) {

    /** JWT absent, expiré ou invalide (HTTP 401). */
    object NonAuthentifie : AppointmentError("Session expirée, veuillez vous reconnecter.")

    /** Accès refusé à une ressource d'un autre patient (HTTP 403). */
    object AccesInterdit : AppointmentError("Accès interdit à ce rendez-vous.")

    /** Médecin, créneau ou rendez-vous introuvable (HTTP 404). */
    object Introuvable : AppointmentError("Rendez-vous ou créneau introuvable.")

    /** Créneau réservé entre-temps (HTTP 409). */
    data class Conflit(val detail: String? = null) : AppointmentError(
        detail?.takeIf { it.isNotBlank() }
            ?: "Ce créneau vient d'être réservé par un autre patient",
    )

    /** Refus métier avec le message renvoyé par le serveur (HTTP 400 / 422). */
    data class Refuse(val detail: String) : AppointmentError(detail)

    /** Erreur interne du service (HTTP 5xx). */
    data class Serveur(val code: Int, val detail: String? = null) : AppointmentError(
        detail?.takeIf { it.isNotBlank() }?.let { "Erreur serveur ($code) : $it" }
            ?: "Erreur serveur ($code), veuillez réessayer.",
    )

    /** Réseau indisponible ou requête interrompue. */
    object Reseau : AppointmentError("Connexion indisponible, vérifiez le réseau.")

    /** Cas non prévu, conservé avec son exception d'origine. */
    data class Inconnu(val erreur: Throwable) : AppointmentError(
        "Une erreur inattendue est survenue.",
        erreur,
    )
}

/**
 * Convertit une exception technique en [AppointmentError] lisible par l'interface.
 *
 * @param moshi Parseur utilisé pour lire le champ `message` du JSON d'erreur du serveur.
 */
internal fun Throwable.commeAppointmentError(moshi: Moshi): AppointmentError = when (this) {
    is AppointmentError -> this
    is HttpException -> when (code()) {
        401 -> AppointmentError.NonAuthentifie
        403 -> AppointmentError.AccesInterdit
        404 -> AppointmentError.Introuvable
        409 -> AppointmentError.Conflit(lireMessageServeur(moshi))
        400, 422 -> AppointmentError.Refuse(
            lireMessageServeur(moshi) ?: "Le serveur a refusé l'opération.",
        )
        in 500..599 -> AppointmentError.Serveur(code(), lireMessageServeur(moshi))
        else -> AppointmentError.Inconnu(this)
    }
    is IOException -> AppointmentError.Reseau
    else -> AppointmentError.Inconnu(this)
}

/** Lit `{"message":"..."}` dans le corps d'erreur, sans jamais faire échouer la conversion. */
private fun HttpException.lireMessageServeur(moshi: Moshi): String? = try {
    response()?.errorBody()?.string()?.let { corps ->
        moshi.adapter(ErreurResponse::class.java).fromJson(corps)?.message
    }
} catch (_: Exception) {
    null
}
