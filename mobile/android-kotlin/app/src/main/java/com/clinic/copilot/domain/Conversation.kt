package com.clinic.copilot.domain

/**
 * Conversation entre l'utilisateur courant et un contact (médecin, cabinet, secrétariat).
 *
 * @property id Identifiant stable de la conversation côté chat-service.
 * @property nomContact Nom affiché du contact dans la liste des messages.
 * @property dernierMessage Aperçu du dernier message, vide si la conversation est neuve.
 * @property dateDernierMessage Horodatage du dernier message, en millisecondes Unix.
 * @property nombreNonLus Nombre de messages reçus non lus ; zéro masque le badge.
 */
data class Conversation(
    val id: String,
    val nomContact: String,
    val dernierMessage: String,
    val dateDernierMessage: Long,
    val nombreNonLus: Int,
)
