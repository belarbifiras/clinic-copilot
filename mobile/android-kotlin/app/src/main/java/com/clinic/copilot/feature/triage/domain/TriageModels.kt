package com.clinic.copilot.feature.triage.domain

/** Message dans le chat de triage IA (US-70 & US-71). */
data class TriageMessage(
    val id: String,
    val role: TriageRole,
    val contenu: String,
    val timestampMs: Long = System.currentTimeMillis(),
    /** Niveau de triage détecté, non-nul dès que l'IA a évalué la sévérité. */
    val severite: NiveauSeverite? = null,
    /** Suggestions rapides proposées par l'IA pour répondre en un clic. */
    val suggestions: List<String> = emptyList(),
    /** Spécialité médicale orientée par l'IA pour naviguer vers l'annuaire (US-71). */
    val orientationSpecialite: String? = null,
    /** Libellé du bouton d'action vers l'annuaire. */
    val orientationActionTexte: String? = null,
    /** Nombre de praticiens disponibles à proximité pour cette spécialité. */
    val medecinsDisponiblesTexte: String? = null,
)

enum class TriageRole { UTILISATEUR, IA }

enum class NiveauSeverite(val libelle: String, val couleurHex: String) {
    CRITIQUE("URGENCE VITALE (SAMU 15)", "#E53E3E"),
    MODERE("MODÉRÉ (SOUS 24H)", "#ED8936"),
    ROUTINIER("CONSULTATION CONSEILLÉE", "#1769E8"),
    ACCOMPAGNEMENT("ACCOMPAGNEMENT IA", "#13B9C1"),
}
