package com.clinic.copilot.ui.theme

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utilitaires de présentation propres à la messagerie.
 *
 * Ils sont placés hors des Composables pour respecter une séparation claire entre
 * le rendu et la mise en forme des données affichées.
 */
object ChatFormatters {

    /**
     * Formate l'horaire d'un message : « HH:mm » pour aujourd'hui, « dd/MM » sinon.
     *
     * @param timestamp Horodatage en millisecondes depuis l'époque Unix.
     * @param maintenant Horodatage courant, injectable pour faciliter les tests.
     * @return Une courte chaîne prête à être affichée.
     */
    fun formaterHeure(timestamp: Long, maintenant: Long = System.currentTimeMillis()): String {
        val memeJour = maintenant - timestamp in 0..MS_PAR_JOUR
        val motif = if (memeJour) "HH:mm" else "dd/MM"
        return SimpleDateFormat(motif, Locale.getDefault()).format(Date(timestamp))
    }

    /**
     * Retourne l'initiale d'un nom de contact pour le repère visuel des cartes.
     *
     * @param nom Nom complet du contact.
     * @return Une lettre majuscule, ou « ? » si le nom est vide.
     */
    fun initiale(nom: String): String = nom.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    /**
     * Fournit la catégorie affichée sous le nom du contact sur une carte.
     *
     * @param nom Nom complet du contact.
     * @return « Médecin traitant », « Secrétariat » ou « Contact médical ».
     */
    fun categorieContact(nom: String): String = when {
        nom.contains("dr", ignoreCase = true) || nom.contains("pr ", ignoreCase = true) -> "Médecin traitant"
        nom.contains("cabinet", ignoreCase = true) ||
            nom.contains("secrét", ignoreCase = true) ||
            nom.contains("secret", ignoreCase = true) -> "Secrétariat"
        else -> "Contact médical"
    }

    private const val MS_PAR_JOUR = 24L * 60L * 60L * 1_000L
}
