package com.clinic.copilot.core.data

import com.clinic.copilot.feature.appointments.domain.DocumentMedical
import com.clinic.copilot.feature.appointments.domain.MomentJour
import com.clinic.copilot.feature.appointments.domain.StatutPrise
import com.clinic.copilot.feature.appointments.domain.TraitementPilulier
import com.clinic.copilot.feature.auth.domain.AuthResult
import com.clinic.copilot.feature.doctors.domain.Doctor
import com.clinic.copilot.feature.doctors.domain.Review
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Modèle de compte utilisateur local.
 */
data class UserAccount(
    val id: String,
    val prenom: String,
    val nom: String,
    val email: String,
    val motDePasse: String,
    val role: String = "PATIENT",
    val numeroSecuMasque: String = "1 89 04 75 ••• •• 42",
    val medecinTraitant: String = "Dr. Marc Laurent",
    val dateNaissance: String = "14/05/1990",
    val telephone: String = "06 12 34 56 78",
    val allergies: List<String> = listOf("Pénicilline (réaction cutanée)", "Pollen de graminées"),
    val antecedents: List<String> = listOf("Appendicectomie (2018)", "Hypertension légère contrôlée"),
    val consentementTriageIA: Boolean = true,
    val rappelsRdvActifs: Boolean = true,
)

/**
 * Modèle de Rendez-vous patient (US-30, US-31, US-32, US-33).
 */
data class LocalRendezVous(
    val id: String,
    val medecinId: String,
    val nomMedecin: String,
    val specialite: String,
    val dateTexte: String,
    val heure: String,
    val lieu: String,
    val statut: StatutRdv = StatutRdv.A_VENIR,
    val motif: String = "Consultation de suivi",
    val teleconsultation: Boolean = false,
    val noteAttribuee: Int? = null,
    val avisLaisse: String? = null,
)

enum class StatutRdv(val libelle: String) {
    A_VENIR("À venir"),
    PASSE("Passé"),
    ANNULE("Annulé"),
}

/**
 * Modèle de notification in-app (US-60, US-61).
 */
data class LocalNotification(
    val id: String,
    val titre: String,
    val message: String,
    val dateTexte: String,
    val estLue: Boolean = false,
    val type: NotificationType = NotificationType.RAPPEL_RDV,
)

enum class NotificationType {
    RAPPEL_RDV,
    PILULIER,
    TRIAGE,
    MESSAGE,
}

/**
 * Base de données locale réactive en mémoire.
 * Fournit l'état unifié pour l'ensemble des modules front-end sans dépendre d'un backend externe.
 */
@Singleton
class ClinicLocalDatabase @Inject constructor() {

    // ── 1. Utilisateurs & Session ───────────────────────────────────────────
    private val comptes = mutableMapOf(
        "patient@demo.fr" to UserAccount(
            id = "patient-demo",
            prenom = "Thomas",
            nom = "Dubois",
            email = "patient@demo.fr",
            motDePasse = "demo1234",
            role = "PATIENT",
        ),
        "sophie.laurent@sante.fr" to UserAccount(
            id = "doc-sophie",
            prenom = "Sophie",
            nom = "Laurent",
            email = "sophie.laurent@sante.fr",
            motDePasse = "password",
            role = "DOCTOR",
        ),
    )

    // Par défaut, aucun utilisateur connecté tant que le login n'a pas eu lieu
    private val _currentUser = MutableStateFlow<UserAccount?>(null)
    val currentUser: StateFlow<UserAccount?> = _currentUser.asStateFlow()

    fun seConnecter(email: String, motDePasse: String): AuthResult {
        val norm = email.trim().lowercase()
        val compte = comptes[norm]
        return when {
            norm.isBlank() || motDePasse.isBlank() ->
                AuthResult.Erreur("Veuillez remplir votre adresse e-mail et votre mot de passe.")
            compte == null ->
                AuthResult.Erreur("Aucun compte n'existe avec cet e-mail. Vérifiez vos identifiants ou inscrivez-vous.")
            compte.motDePasse != motDePasse ->
                AuthResult.Erreur("Mot de passe incorrect. Veuillez vérifier votre saisie.")
            else -> {
                _currentUser.value = compte
                AuthResult.Succes(userId = compte.id)
            }
        }
    }

    fun sInscrire(prenom: String, nom: String, email: String, motDePasse: String): AuthResult {
        val norm = email.trim().lowercase()
        return when {
            prenom.isBlank() || nom.isBlank() ->
                AuthResult.Erreur("Veuillez renseigner votre nom et prénom.")
            norm.isBlank() || !norm.contains("@") ->
                AuthResult.Erreur("Adresse e-mail invalide.")
            motDePasse.length < 6 ->
                AuthResult.Erreur("Le mot de passe doit contenir au moins 6 caractères.")
            comptes.containsKey(norm) ->
                AuthResult.Erreur("Un compte existe déjà avec cette adresse e-mail.")
            else -> {
                val newAccount = UserAccount(
                    id = "user-${System.currentTimeMillis()}",
                    prenom = prenom.trim().replaceFirstChar { it.uppercase() },
                    nom = nom.trim().replaceFirstChar { it.uppercase() },
                    email = norm,
                    motDePasse = motDePasse,
                    role = "PATIENT",
                )
                comptes[norm] = newAccount
                _currentUser.value = newAccount
                AuthResult.Succes(userId = newAccount.id)
            }
        }
    }

    fun seDeconnecter() {
        _currentUser.value = null
    }

    // ── 2. Annuaire des Médecins ────────────────────────────────────────────
    private val _doctors = MutableStateFlow<List<Doctor>>(initialDoctors())
    val doctors: StateFlow<List<Doctor>> = _doctors.asStateFlow()

    fun ajouterAvis(medecinId: String, note: Int, commentaire: String, nomAuteur: String = "Patient vérifié") {
        _doctors.update { docs ->
            docs.map { doc ->
                if (doc.id == medecinId) {
                    val nouvelAvis = Review(
                        id = "rev-${System.currentTimeMillis()}",
                        auteur = nomAuteur,
                        note = note,
                        date = "Aujourd'hui",
                        commentaire = commentaire,
                    )
                    val nouveauxAvis = listOf(nouvelAvis) + doc.avis
                    val nouvelleNote = ((doc.note * doc.nombreAvis + note) / (doc.nombreAvis + 1))
                    doc.copy(
                        note = String.format(java.util.Locale.US, "%.1f", nouvelleNote).toDouble(),
                        nombreAvis = doc.nombreAvis + 1,
                        avis = nouveauxAvis,
                    )
                } else doc
            }
        }
    }

    // ── 3. Rendez-vous Patient ──────────────────────────────────────────────
    private val _rendezVous = MutableStateFlow<List<LocalRendezVous>>(initialRendezVous())
    val rendezVous: StateFlow<List<LocalRendezVous>> = _rendezVous.asStateFlow()

    fun reserverRdv(
        doctor: Doctor,
        dateTexte: String,
        heure: String,
        motif: String = "Consultation de suivi",
        teleconsultation: Boolean = false,
    ): LocalRendezVous {
        val rdv = LocalRendezVous(
            id = "rdv-${System.currentTimeMillis()}",
            medecinId = doctor.id,
            nomMedecin = doctor.nomComplet,
            specialite = doctor.specialite,
            dateTexte = "$dateTexte à $heure",
            heure = heure,
            lieu = if (teleconsultation) "Téléconsultation en ligne sécurisée" else doctor.adresse,
            statut = StatutRdv.A_VENIR,
            motif = motif,
            teleconsultation = teleconsultation,
        )
        _rendezVous.update { listOf(rdv) + it }

        // Ajoute une notification de confirmation
        ajouterNotification(
            titre = "Rendez-vous confirmé",
            message = "Votre consultation avec ${doctor.nomComplet} le $dateTexte à $heure a été enregistrée.",
            type = NotificationType.RAPPEL_RDV,
        )
        return rdv
    }

    fun annulerRdv(rdvId: String) {
        var nomMed = ""
        _rendezVous.update { liste ->
            liste.map { rdv ->
                if (rdv.id == rdvId) {
                    nomMed = rdv.nomMedecin
                    rdv.copy(statut = StatutRdv.ANNULE)
                } else rdv
            }
        }
        if (nomMed.isNotBlank()) {
            ajouterNotification(
                titre = "Rendez-vous annulé",
                message = "Le rendez-vous avec $nomMed a bien été annulé.",
                type = NotificationType.RAPPEL_RDV,
            )
        }
    }

    fun noterRdv(rdvId: String, note: Int, commentaire: String) {
        var medecinId = ""
        val nomPatient = _currentUser.value?.let { "${it.prenom} ${it.nom.first()}." } ?: "Patient"
        _rendezVous.update { liste ->
            liste.map { rdv ->
                if (rdv.id == rdvId) {
                    medecinId = rdv.medecinId
                    rdv.copy(noteAttribuee = note, avisLaisse = commentaire)
                } else rdv
            }
        }
        if (medecinId.isNotBlank()) {
            ajouterAvis(medecinId, note, commentaire, nomPatient)
        }
    }

    // ── 4. Pilulier et Traitements ──────────────────────────────────────────
    private val _traitements = MutableStateFlow<List<TraitementPilulier>>(initialTraitements())
    val traitements: StateFlow<List<TraitementPilulier>> = _traitements.asStateFlow()

    fun validerPriseMedicament(id: String) {
        _traitements.update { liste ->
            liste.map {
                if (it.id == id) it.copy(statutPrise = StatutPrise.PRIS, creneauHoraire = "Pris à l'instant") else it
            }
        }
    }

    // ── 5. Documents Médicaux ───────────────────────────────────────────────
    private val _documents = MutableStateFlow<List<DocumentMedical>>(initialDocuments())
    val documents: StateFlow<List<DocumentMedical>> = _documents.asStateFlow()

    // ── 6. Notifications In-App ─────────────────────────────────────────────
    private val _notifications = MutableStateFlow<List<LocalNotification>>(initialNotifications())
    val notifications: StateFlow<List<LocalNotification>> = _notifications.asStateFlow()

    fun ajouterNotification(titre: String, message: String, type: NotificationType) {
        val notif = LocalNotification(
            id = "notif-${System.currentTimeMillis()}",
            titre = titre,
            message = message,
            dateTexte = "À l'instant",
            estLue = false,
            type = type,
        )
        _notifications.update { listOf(notif) + it }
    }

    fun marquerNotificationCommeLue(id: String) {
        _notifications.update { liste ->
            liste.map { if (it.id == id) it.copy(estLue = true) else it }
        }
    }

    fun toutMarquerNotificationsCommeLues() {
        _notifications.update { liste -> liste.map { it.copy(estLue = true) } }
    }

    // ── Données initiales riches de démonstration ───────────────────────────
    private fun initialDoctors(): List<Doctor> = listOf(
        Doctor(
            id = "doc-1",
            nomComplet = "Dr. Éléonore Garnier",
            specialite = "Médecin Généraliste",
            secteur = "Secteur 1 (Sans dépassement)",
            note = 4.9,
            nombreAvis = 132,
            adresse = "14 rue de Vaugirard, Paris 15e",
            distanceKm = 0.8,
            disponibleAujourdhui = true,
            teleconsultation = true,
            prochainsCreneaux = listOf("14:30", "15:15", "16:45", "Demain 09:00"),
            bio = "Médecin généraliste d'expérience, spécialisée en suivi familial, bilans préventifs et coordination des soins.",
            diplomes = "Diplôme d'État de Docteur en Médecine (Univ. Paris Cité), D.U. Urgences Pédiatriques",
            tarif = "26,50 € (Conventionné secteur 1, tiers-payant Carte Vitale)",
            mapX = 0.48f,
            mapY = 0.38f,
            avis = listOf(
                Review("rev-101", "Claire M.", 5, "Il y a 3 jours", "Praticienne très à l'écoute et ponctuelle. Explications très claires."),
                Review("rev-102", "Julien D.", 5, "Il y a 1 semaine", "Consultation de qualité, cabinet moderne et accueillant."),
            ),
        ),
        Doctor(
            id = "doc-2",
            nomComplet = "Dr. Marc Laurent",
            specialite = "Cardiologie",
            secteur = "Secteur 1",
            note = 4.8,
            nombreAvis = 98,
            adresse = "Clinique Pasteur, Bât B, Paris 14e",
            distanceKm = 1.6,
            disponibleAujourdhui = true,
            teleconsultation = true,
            prochainsCreneaux = listOf("16:00", "17:30", "Demain 11:00"),
            bio = "Cardiologue clinicien, ancien chef de clinique. Prise en charge de l'hypertension, bilans coronariens et prévention cardiovasculaire.",
            diplomes = "D.E.S Cardiologie et Maladies Vasculaires, Ancien interne des Hôpitaux de Paris",
            tarif = "50,00 € (Base remboursée CPAM)",
            mapX = 0.35f,
            mapY = 0.65f,
            avis = listOf(
                Review("rev-201", "Michel T.", 5, "Il y a 2 semaines", "Excellent cardiologue, examen complet avec ECG sur place."),
            ),
        ),
        Doctor(
            id = "doc-3",
            nomComplet = "Dr. Thomas Bernard",
            specialite = "Neurologie",
            secteur = "Secteur 2",
            note = 4.9,
            nombreAvis = 85,
            adresse = "28 boulevard Raspail, Paris 6e",
            distanceKm = 2.1,
            disponibleAujourdhui = true,
            teleconsultation = false,
            prochainsCreneaux = listOf("11:30", "15:00", "Vendredi 14:00"),
            bio = "Neurologue spécialiste des céphalées chroniques, migraines rebelles, vertiges et troubles neurologiques périphériques.",
            diplomes = "D.E.S Neurologie, Praticien Attaché Hôpital Pitié-Salpêtrière",
            tarif = "65,00 € (Secteur 2 - Contrat OPTAM)",
            mapX = 0.68f,
            mapY = 0.28f,
            avis = listOf(
                Review("rev-301", "Nathalie B.", 5, "Il y a 4 jours", "A enfin trouvé un traitement efficace pour mes migraines. Merci docteur !"),
            ),
        ),
        Doctor(
            id = "doc-4",
            nomComplet = "Dr. Nadia Benali",
            specialite = "Dermatologie",
            secteur = "Secteur 1",
            note = 4.7,
            nombreAvis = 64,
            adresse = "45 avenue Jean Jaurès, Boulogne-Billancourt",
            distanceKm = 3.2,
            disponibleAujourdhui = false,
            teleconsultation = true,
            prochainsCreneaux = listOf("Jeudi 09:30", "Jeudi 11:15", "Vendredi 10:00"),
            bio = "Dermatologue spécialisée en dépistage des lésions cutanées, eczéma, acné de l'adulte et dermatologie générale.",
            diplomes = "Diplôme Universitaire de Dermoscopie, D.E.S Dermatologie-Vénérologie",
            tarif = "35,00 € (Secteur 1 conventionné)",
            mapX = 0.22f,
            mapY = 0.45f,
            avis = listOf(
                Review("rev-401", "Antoine P.", 4, "Il y a 1 mois", "Très professionnelle, diagnostic rapide et efficace."),
            ),
        ),
        Doctor(
            id = "doc-5",
            nomComplet = "Dr. Camille Roux",
            specialite = "Pédiatrie",
            secteur = "Secteur 1",
            note = 5.0,
            nombreAvis = 112,
            adresse = "8 rue Cassini, Paris 14e",
            distanceKm = 1.9,
            disponibleAujourdhui = true,
            teleconsultation = true,
            prochainsCreneaux = listOf("14:00", "15:30", "17:00"),
            bio = "Pédiatre attentionnée assurant le suivi de la naissance à l'adolescence, vaccinations et urgences pédiatriques courantes.",
            diplomes = "D.E.S Pédiatrie, Ancien Chef de Clinique Hôpital Necker-Enfants Malades",
            tarif = "35,00 € (Secteur 1 conventionné)",
            mapX = 0.55f,
            mapY = 0.72f,
            avis = listOf(
                Review("rev-501", "Sabrina K.", 5, "Il y a 2 jours", "La meilleure pédiatre ! Douce avec les enfants et toujours rassurante."),
            ),
        ),
        Doctor(
            id = "doc-6",
            nomComplet = "Dr. Julien Morel",
            specialite = "Ophtalmologie",
            secteur = "Secteur 2",
            note = 4.8,
            nombreAvis = 76,
            adresse = "92 rue Lecourbe, Paris 15e",
            distanceKm = 1.1,
            disponibleAujourdhui = true,
            teleconsultation = false,
            prochainsCreneaux = listOf("16:15", "17:00", "Demain 10:30"),
            bio = "Chirurgien ophtalmologiste, bilan de la vue, lentilles de contact, suivi du glaucome et pathologies de la rétine.",
            diplomes = "D.E.S Ophtalmologie, Société Française d'Ophtalmologie",
            tarif = "60,00 € (Secteur 2)",
            mapX = 0.38f,
            mapY = 0.22f,
            avis = listOf(
                Review("rev-601", "Gérard L.", 5, "Il y a 3 semaines", "Matériel de pointe pour l'examen du fond d'œil."),
            ),
        ),
    )

    private fun initialRendezVous(): List<LocalRendezVous> = listOf(
        LocalRendezVous(
            id = "rdv-init-1",
            medecinId = "doc-2",
            nomMedecin = "Dr. Marc Laurent",
            specialite = "Cardiologie",
            dateTexte = "Demain à 14h30",
            heure = "14h30",
            lieu = "Clinique Pasteur, Bâtiment B – Salle 204",
            statut = StatutRdv.A_VENIR,
            motif = "Contrôle annuel de tension artérielle",
        ),
        LocalRendezVous(
            id = "rdv-init-2",
            medecinId = "doc-1",
            nomMedecin = "Dr. Éléonore Garnier",
            specialite = "Médecine Générale",
            dateTexte = "15 Nov 2026 à 10h15",
            heure = "10h15",
            lieu = "14 rue de Vaugirard, Paris 15e",
            statut = StatutRdv.PASSE,
            motif = "Consultation générale & ordonnance",
            noteAttribuee = null, // Prêt à recevoir un avis
        ),
    )

    private fun initialTraitements(): List<TraitementPilulier> = listOf(
        TraitementPilulier(
            id = "t1",
            nom = "Amoxicilline 500mg",
            posologie = "1 gélule matin et soir au cours du repas",
            creneauHoraire = "08:15",
            momentJour = MomentJour.MATIN,
            statutPrise = StatutPrise.PRIS,
            prescritPar = "Dr. Éléonore Garnier",
            progressionJours = "Jour 4 / 6",
        ),
        TraitementPilulier(
            id = "t2",
            nom = "Doliprane 1000mg",
            posologie = "Si douleur ou fièvre (espacer de 6h min)",
            creneauHoraire = "14:00",
            momentJour = MomentJour.MIDI,
            statutPrise = StatutPrise.A_PRENDRE,
            prescritPar = "Dr. Éléonore Garnier",
            progressionJours = "Si besoin",
        ),
        TraitementPilulier(
            id = "t3",
            nom = "Kardégic 75mg",
            posologie = "1 sachet par jour au dîner",
            creneauHoraire = "20:00",
            momentJour = MomentJour.SOIR,
            statutPrise = StatutPrise.A_VENIR,
            prescritPar = "Dr. Marc Laurent",
            progressionJours = "Traitement de fond",
        ),
    )

    private fun initialDocuments(): List<DocumentMedical> = listOf(
        DocumentMedical(
            id = "doc-1",
            titre = "Compte-rendu Consultation Cardiologie",
            date = "24 Oct 2026",
            medecin = "Dr. Marc Laurent",
            tailleFichier = "PDF · 1.2 Mo",
        ),
        DocumentMedical(
            id = "doc-2",
            titre = "Ordonnance Bilan Sanguin & NFS",
            date = "15 Sep 2026",
            medecin = "Laboratoire d'Eylau",
            tailleFichier = "PDF · 850 Ko",
        ),
        DocumentMedical(
            id = "doc-3",
            titre = "Bilan Triage Clinique Copilot",
            date = "12 Nov 2026",
            medecin = "Clinic Copilot IA",
            tailleFichier = "PDF · 420 Ko",
        ),
    )

    private fun initialNotifications(): List<LocalNotification> = listOf(
        LocalNotification(
            id = "notif-1",
            titre = "Rappel de consultation",
            message = "Votre rendez-vous avec le Dr. Marc Laurent a lieu demain à 14h30.",
            dateTexte = "Il y a 2h",
            estLue = false,
            type = NotificationType.RAPPEL_RDV,
        ),
        LocalNotification(
            id = "notif-2",
            titre = "Pilulier du midi",
            message = "Il est l'heure de votre prise : Doliprane 1000mg.",
            dateTexte = "Il y a 3h",
            estLue = false,
            type = NotificationType.PILULIER,
        ),
        LocalNotification(
            id = "notif-3",
            titre = "Bilan Triage IA disponible",
            message = "Votre synthèse d'orientation médicale est enregistrée dans votre dossier.",
            dateTexte = "Hier",
            estLue = true,
            type = NotificationType.TRIAGE,
        ),
    )
}
