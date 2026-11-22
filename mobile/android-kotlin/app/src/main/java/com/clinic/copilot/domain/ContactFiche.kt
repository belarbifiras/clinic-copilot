package com.clinic.copilot.domain

/**
 * Coordonnées minimales d'un contact, affichées par l'Activity de fiche.
 *
 * Les numéros de téléphone de cette feature sont fictifs et servent uniquement
 * à démontrer l'Intent implicite `ACTION_DIAL`.
 *
 * @property nom Nom complet du contact.
 * @property specialite Rôle ou spécialité affichée sous le nom.
 * @property telephone Numéro de téléphone fictif au format lisible.
 */
data class ContactFiche(
    val nom: String,
    val specialite: String,
    val telephone: String,
)
