package com.clinic.copilot.ui.booking

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract

/**
 * Déclencheur d'Intent implicite vers une application calendrier.
 *
 * Aucune logique métier n'est effectuée ici : l'objet ne construit que l'Intent
 * d'insertion et signale à l'écran si aucune application n'est disponible.
 */
object CalendarIntentHelper {

    /**
     * Ouvre l'insertion d'un événement pré-rempli dans le calendrier du téléphone.
     *
     * @param contexte Contexte Android utilisé pour démarrer l'activité.
     * @param titre Titre de l'événement, par exemple « Rendez-vous - Dr. X ».
     * @param lieu Lieu du rendez-vous, éventuellement nul.
     * @param debut Timestamp de début en millisecondes.
     * @param fin Timestamp de fin en millisecondes.
     * @return `true` si une application calendrier a été lancée, `false` sinon.
     */
    fun ouvrirCalendrier(
        contexte: Context,
        titre: String,
        lieu: String?,
        debut: Long,
        fin: Long,
    ): Boolean {
        val intention = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, titre)
            putExtra(CalendarContract.Events.EVENT_LOCATION, lieu.orEmpty())
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, debut)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, fin)
            putExtra(CalendarContract.Events.ALL_DAY, false)
        }
        return try {
            contexte.startActivity(intention)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }
}
