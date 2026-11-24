package com.clinic.copilot.feature.profile.domain

/** Données complètes du profil patient (US-13). */
data class PatientProfile(
    val nomComplet: String = "Thomas Dubois",
    val email: String = "patient@demo.fr",
    val age: Int = 34,
    val dateNaissance: String = "14/05/1990",
    val numeroSecuMasque: String = "1 89 *** 12",
    val groupeSanguin: String = "O+",
    val dossierVerifie: Boolean = true,
    val consultationsCount: Int = 4,
    val traitementsCount: Int = 2,
    val medecinReferentCount: Int = 1,
    val medecinTraitant: String = "Dr. Marc Laurent",
    val medecinTraitantNom: String = "Dr. Marc Laurent",
    val medecinTraitantSpecialite: String = "Médecine générale",
    val mutuelleNom: String = "Harmonie Mutuelle",
    val mutuelleValidite: String = "31/12/2026",
    val allergies: List<String> = listOf("Pénicilline"),
    val antecedents: List<String> = listOf("Appendicectomie (2018)"),
    val consentementTriageIA: Boolean = true,
    val hebergementHDS: Boolean = true,
    val praticiensPartagesCount: Int = 2,
    val rappelsRdvActifs: Boolean = true,
)
