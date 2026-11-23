package com.clinic.copilot.feature.appointments.domain

/** Traitement en cours du patient (Pilulier / Dossier médical). */
data class TraitementPilulier(
    val id: String,
    val nom: String,
    val posologie: String,
    val creneauHoraire: String, // "08:00", "14:00", "20:30"
    val momentJour: MomentJour,
    val statutPrise: StatutPrise,
    val prescritPar: String,
    val progressionJours: String, // "Jour 4 / 6"
)

enum class MomentJour(val libelle: String, val heureDefaut: String) {
    MATIN("Matin", "08:00"),
    MIDI("Midi", "14:00"),
    SOIR("Soir", "20:30"),
}

enum class StatutPrise {
    PRIS,
    A_PRENDRE,
    A_VENIR,
}

data class DocumentMedical(
    val id: String,
    val titre: String,
    val date: String,
    val medecin: String,
    val tailleFichier: String,
)
