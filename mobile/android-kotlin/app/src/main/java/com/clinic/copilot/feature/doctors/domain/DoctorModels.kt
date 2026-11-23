package com.clinic.copilot.feature.doctors.domain

/** Praticien de l'annuaire (US-20, US-22). */
data class Doctor(
    val id: String,
    val nomComplet: String,
    val specialite: String,
    val secteur: String = "Secteur 1",
    val note: Double = 4.9,
    val nombreAvis: Int = 120,
    val adresse: String = "Paris 15e",
    val distanceKm: Double = 1.2,
    val disponibleAujourdhui: Boolean = true,
    val teleconsultation: Boolean = true,
    val prochainsCreneaux: List<String> = listOf("14:30", "15:15", "16:00"),
)

enum class FiltreMedecin(val label: String) {
    TOUS("Tous"),
    DISPO_AUJOURDHUI("Dispo aujourd'hui"),
    TELECONSULTATION("Téléconsultation"),
}
