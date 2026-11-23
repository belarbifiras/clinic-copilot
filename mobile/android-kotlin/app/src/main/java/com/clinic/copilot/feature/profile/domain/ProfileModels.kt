package com.clinic.copilot.feature.profile.domain

/** Données complètes du profil patient (US-13). */
data class PatientProfile(
    val nomComplet: String = "Thomas Dubois",
    val age: Int = 34,
    val numeroSecuMasque: String = "1 89 *** 12",
    val dossierVerifie: Boolean = true,
    val consultationsCount: Int = 4,
    val traitementsCount: Int = 2,
    val medecinReferentCount: Int = 1,
    val medecinTraitantNom: String = "Dr. Marc Laurent",
    val medecinTraitantSpecialite: String = "Médecine générale",
    val mutuelleNom: String = "Harmonie Mutuelle",
    val mutuelleValidite: String = "31/12/2026",
    val consentementTriageIA: Boolean = true,
    val hebergementHDS: Boolean = true,
    val praticiensPartagesCount: Int = 2,
    val rappelsRdvActifs: Boolean = true,
)
