package com.clinic.copilot.feature.home.domain

import java.time.LocalDate
import java.time.LocalTime

/** Données affichées sur l'écran d'accueil du patient. */
data class HomeData(
    val prenomPatient: String,
    val prochainRdv: ProchainRdv?,
    val medicamentsJour: List<MedicamentJour>,
    val triageSauvegarde: Boolean,
)

data class ProchainRdv(
    val nomMedecin: String,
    val specialite: String,
    val estConfirme: Boolean,
    val dateTexte: String,
    val heure: String,
    val lieu: String,
)

data class MedicamentJour(
    val nom: String,
    val posologie: String,
    val pris: Boolean,
)
