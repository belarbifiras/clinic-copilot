package com.clinic.copilot.feature.triage.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clinic.copilot.core.data.ClinicLocalDatabase
import com.clinic.copilot.feature.triage.domain.NiveauSeverite
import com.clinic.copilot.feature.triage.domain.TriageMessage
import com.clinic.copilot.feature.triage.domain.TriageRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class TriageUiState(
    val messages: List<TriageMessage> = emptyList(),
    val isTyping: Boolean = false,
    val saisie: String = "",
    val medecinsDisponibles: Int = 4,
    val creneauxTrouves: Int = 8,
    val specialiteConclue: String? = null,
)

/**
 * ViewModel du Triage IA – Conforme aux US-70 (Interface Triage) et US-71 (Orientation spécialité).
 */
@HiltViewModel
class TriageViewModel @Inject constructor(
    private val database: ClinicLocalDatabase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TriageUiState())
    val uiState: StateFlow<TriageUiState> = _uiState.asStateFlow()

    // Gestion d'état de l'arbre décisionnel
    private var etapeDiscussion = 0
    private var symptomePrincipal = ""

    init {
        val prenom = database.currentUser.value?.prenom ?: "Thomas"
        val accueil = iaMessage(
            contenu = "Bonjour $prenom ! Je suis votre Copilot santé certifié HDS. Décrivez-moi vos symptômes ou choisissez une situation fréquente pour démarrer l'évaluation clinique.",
            suggestions = listOf(
                "Maux de tête persistants",
                "Fièvre & courbatures",
                "Douleurs thoraciques",
                "Éruption cutanée",
                "Douleurs articulaires",
            ),
        )
        _uiState.update { it.copy(messages = listOf(accueil)) }
    }

    fun mettreAJourSaisie(texte: String) {
        _uiState.update { it.copy(saisie = texte) }
    }

    fun envoyerMessage() {
        val texte = _uiState.value.saisie.trim()
        if (texte.isBlank()) return

        val msgUtilisateur = TriageMessage(
            id = UUID.randomUUID().toString(),
            role = TriageRole.UTILISATEUR,
            contenu = texte,
        )
        _uiState.update { it.copy(messages = it.messages + msgUtilisateur, saisie = "", isTyping = true) }

        viewModelScope.launch {
            delay(800) // simulation de réflexion IA
            val reponse = traiterReponseIA(texte)
            _uiState.update {
                it.copy(
                    messages = it.messages + reponse,
                    isTyping = false,
                    specialiteConclue = reponse.orientationSpecialite ?: it.specialiteConclue,
                )
            }
        }
    }

    fun envoyerSuggestion(texte: String) {
        _uiState.update { it.copy(saisie = texte) }
        envoyerMessage()
    }

    // ── Moteur d'évaluation clinique et d'orientation ───────────────────────

    private fun traiterReponseIA(input: String): TriageMessage {
        val bas = input.lowercase()

        // 1. Détection d'urgence vitale immédiate
        if (bas.contains("thorac") || bas.contains("poitrin") || bas.contains("infarctus") || bas.contains("bras gauche")) {
            symptomePrincipal = "thoracique"
            return iaMessage(
                contenu = "⚠ ALERTE CLINIQUE : Une douleur thoracique persistante ou irradiante nécessite une prise en charge urgente. Si la douleur est aiguë, contactez immédiatement le SAMU (15). Dans le cas d'un suivi, consultez un cardiologue sans délai.",
                severite = NiveauSeverite.CRITIQUE,
                suggestions = listOf("Contacter le 15", "Consulter en Cardiologie"),
                orientationSpecialite = "Cardiologie",
                orientationActionTexte = "Consulter les Cardiologues disponibles",
                medecinsDisponiblesTexte = "Dr. Marc Laurent disponible pour consultation",
            )
        }

        // Progression de l'évaluation
        when (etapeDiscussion) {
            0 -> {
                // Étape 0 : Détection du symptôme initial
                symptomePrincipal = when {
                    bas.contains("migraine") || bas.contains("céphal") || bas.contains("tête") -> "migraine"
                    bas.contains("fièvre") || bas.contains("courbature") || bas.contains("grippe") -> "fievre"
                    bas.contains("peau") || bas.contains("éruption") || bas.contains("bouton") || bas.contains("dermat") -> "peau"
                    bas.contains("articulaire") || bas.contains("genou") || bas.contains("dos") -> "articulaire"
                    else -> "general"
                }
                etapeDiscussion = 1
                return iaMessage(
                    contenu = "Je prends note de ces informations. Depuis combien de temps ces symptômes durent-ils et quelle en est l'intensité ?",
                    severite = NiveauSeverite.MODERE,
                    suggestions = listOf("Depuis hier (modéré)", "Depuis 3 jours", "Depuis plus d'une semaine"),
                )
            }
            1 -> {
                // Étape 1 : Réception de la durée et demande des signes associés
                etapeDiscussion = 2
                return iaMessage(
                    contenu = "Bien noté. Ressentez-vous d'autres manifestations associées (comme des vertiges, nausées, troubles de la vision ou fatigue anormale) ?",
                    severite = NiveauSeverite.MODERE,
                    suggestions = listOf("Oui, vertiges ou nausées", "Sensibilité à la lumière", "Aucun autre symptôme"),
                )
            }
            else -> {
                // Étape 2+ : Conclusion clinique et recommandation de spécialité (US-70 & US-71)
                etapeDiscussion = 0 // Réinitialisation pour prochaine consultation
                return genererConclusionClinique(symptomePrincipal)
            }
        }
    }

    private fun genererConclusionClinique(symptome: String): TriageMessage {
        return when (symptome) {
            "migraine" -> iaMessage(
                contenu = "SYNTHÈSE CLINIQUE COPILOT :\n" +
                    "Vos céphalées persistantes avec signes associés nécessitent un avis spécialisé sous 24h à 48h. Une consultation en Neurologie permettra d'écarter toute anomalie et de vous prescrire un traitement préventif et de crise.",
                severite = NiveauSeverite.MODERE,
                suggestions = listOf("Prendre rendez-vous", "Téléconsultation"),
                orientationSpecialite = "Neurologie",
                orientationActionTexte = "Consulter les spécialistes en Neurologie",
                medecinsDisponiblesTexte = "Dr. Thomas Bernard (Neurologue) a 2 créneaux disponibles cette semaine",
            )
            "fievre" -> iaMessage(
                contenu = "SYNTHÈSE CLINIQUE COPILOT :\n" +
                    "Le tableau clinique évoque un syndrome viral saisonnier. Une auscultation par un Médecin Généraliste est recommandée sous 24h pour adapter le traitement symptomatique.",
                severite = NiveauSeverite.ROUTINIER,
                suggestions = listOf("Voir les généralistes", "Créneaux d'aujourd'hui"),
                orientationSpecialite = "Médecin Généraliste",
                orientationActionTexte = "Consulter les Médecins Généralistes",
                medecinsDisponiblesTexte = "Dr. Éléonore Garnier disponible dès aujourd'hui à 14h30",
            )
            "peau" -> iaMessage(
                contenu = "SYNTHÈSE CLINIQUE COPILOT :\n" +
                    "Une évaluation dermatologique est conseillée afin d'examiner les lésions cutanées et vous prescrire une prise en charge topique adaptée.",
                severite = NiveauSeverite.ROUTINIER,
                suggestions = listOf("Prendre RDV Dermatologue"),
                orientationSpecialite = "Dermatologie",
                orientationActionTexte = "Consulter les Dermatologues",
                medecinsDisponiblesTexte = "Dr. Nadia Benali (Dermatologue) disponible cette semaine",
            )
            else -> iaMessage(
                contenu = "SYNTHÈSE CLINIQUE COPILOT :\n" +
                    "D'après vos réponses, une consultation de Médecine Générale est conseillée sous 48h pour un examen clinique complet.",
                severite = NiveauSeverite.MODERE,
                suggestions = listOf("Voir l'annuaire médical"),
                orientationSpecialite = "Médecin Généraliste",
                orientationActionTexte = "Prendre RDV avec un praticien",
                medecinsDisponiblesTexte = "4 médecins disponibles à proximité",
            )
        }
    }

    private fun iaMessage(
        contenu: String,
        severite: NiveauSeverite? = null,
        suggestions: List<String> = emptyList(),
        orientationSpecialite: String? = null,
        orientationActionTexte: String? = null,
        medecinsDisponiblesTexte: String? = null,
    ) = TriageMessage(
        id = UUID.randomUUID().toString(),
        role = TriageRole.IA,
        contenu = contenu,
        severite = severite,
        suggestions = suggestions,
        orientationSpecialite = orientationSpecialite,
        orientationActionTexte = orientationActionTexte,
        medecinsDisponiblesTexte = medecinsDisponiblesTexte,
    )
}
