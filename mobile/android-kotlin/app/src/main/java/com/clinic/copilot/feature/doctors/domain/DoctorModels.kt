package com.clinic.copilot.feature.doctors.domain

/** Modèle d'avis patient vérifié (US-80). */
data class Review(
    val id: String,
    val auteur: String,
    val note: Int,
    val date: String,
    val commentaire: String,
)

/** Praticien complet de l'annuaire (US-20, US-22). */
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
    val bio: String = "Praticien qualifié conventionné, à l'écoute des patients.",
    val diplomes: String = "Diplôme d'État de Docteur en Médecine",
    val tarif: String = "26,50 € (Conventionné secteur 1)",
    val mapX: Float = 0.5f, // Position X normalisée (0..1) pour la vue carte
    val mapY: Float = 0.5f, // Position Y normalisée (0..1) pour la vue carte
    val avis: List<Review> = emptyList(),
)

enum class FiltreMedecin(val label: String) {
    TOUS("Tous"),
    DISPO_AUJOURDHUI("Dispo aujourd'hui"),
    TELECONSULTATION("Téléconsultation"),
}
