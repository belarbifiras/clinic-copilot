package com.clinic.copilot.feature.triage.domain

/** Message dans le chat de triage IA. */
data class TriageMessage(
    val id: String,
    val role: TriageRole,
    val contenu: String,
    val timestampMs: Long = System.currentTimeMillis(),
    /** Niveau de triage détecté, non-nul dès que l'IA a évalué la sévérité. */
    val severite: NiveauSeverite? = null,
    /** Suggestions rapides proposées par l'IA pour répondre en un clic. */
    val suggestions: List<String> = emptyList(),
)

enum class TriageRole { UTILISATEUR, IA }

enum class NiveauSeverite(val libelle: String, val couleurHex: String) {
    CRITIQUE("CRITIQUE", "#E53E3E"),
    MODERE("MODÉRÉ (SOUS 24H)", "#ED8936"),
    ROUTINIER("ROUTINIER", "#1769E8"),
    ACCOMPAGNEMENT("ACCOMPAGNEMENT IA", "#13B9C1"),
}
