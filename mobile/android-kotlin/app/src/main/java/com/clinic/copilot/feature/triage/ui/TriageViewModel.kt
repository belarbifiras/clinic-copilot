package com.clinic.copilot.feature.triage.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clinic.copilot.feature.triage.domain.NiveauSeverite
import com.clinic.copilot.feature.triage.domain.TriageMessage
import com.clinic.copilot.feature.triage.domain.TriageRole
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TriageUiState(
    val messages: List<TriageMessage> = emptyList(),
    val isTyping: Boolean = false,
    val saisie: String = "",
    val medecinsDisponibles: Int = 2,
    val creneauxTrouves: Int = 2,
)

/**
 * ViewModel de l'écran Triage IA – Membre 2, écran 2/2.
 *
 * Simule une conversation avec l'IA de triage : délai de frappe fictif,
 * détection de mots-clés pour proposer une sévérité, suggestions rapides.
 * À brancher sur ai-assessment-service en production.
 */
@HiltViewModel
class TriageViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(TriageUiState())
    val uiState: StateFlow<TriageUiState> = _uiState.asStateFlow()

    init {
        // Message d'accueil de l'IA
        val accueil = iaMessage(
            contenu = "Bonjour Thomas ! Je suis votre Copilot santé. Décrivez-moi vos sensations ou touchez une suggestion fréquente ci-dessous pour démarrer.",
            suggestions = listOf("Maux de tête persistants", "Fièvre & courbatures", "Douleurs thoraciques"),
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
            delay(1200) // simulation frappe IA
            val reponse = genererReponseIA(texte)
            _uiState.update { it.copy(messages = it.messages + reponse, isTyping = false) }
        }
    }

    fun envoyerSuggestion(texte: String) {
        _uiState.update { it.copy(saisie = texte) }
        envoyerMessage()
    }

    // ── Simulation IA ────────────────────────────────────────────────────────

    private fun genererReponseIA(input: String): TriageMessage {
        val bas = input.lowercase()
        return when {
            bas.contains("migraine") || bas.contains("céphalée") || bas.contains("tête") ->
                iaMessage(
                    contenu = "Je note ces éléments avec précision. Ressentez-vous également des nausées ou une raideur particulière au niveau de la nuque ?",
                    severite = NiveauSeverite.MODERE,
                    suggestions = listOf("Oui", "Non", "Modérément"),
                )
            bas.contains("fièvre") || bas.contains("courbature") ->
                iaMessage(
                    contenu = "Depuis combien de temps avez-vous de la fièvre ? Avez-vous pris votre température récemment ?",
                    suggestions = listOf("Depuis hier", "Depuis 3 jours", "Je n'ai pas de thermomètre"),
                )
            bas.contains("douleur") && (bas.contains("thorac") || bas.contains("poitrin")) ->
                iaMessage(
                    contenu = "⚠ Une douleur thoracique peut être urgente. Appelez le 15 si la douleur est intense ou irradie vers le bras gauche.",
                    severite = NiveauSeverite.CRITIQUE,
                )
            bas.contains("oui") || bas.contains("non") || bas.contains("moderem") ->
                iaMessage(
                    contenu = "Merci. Sur la base de vos symptômes, j'estime que vous devriez consulter dans les 24h. Voici les médecins disponibles près de chez vous.",
                    severite = NiveauSeverite.MODERE,
                    suggestions = listOf("Voir les médecins", "Prendre rendez-vous"),
                )
            else ->
                iaMessage(
                    contenu = "Pouvez-vous me décrire vos symptômes plus précisément ? Depuis combien de temps durent-ils ?",
                    suggestions = listOf("Depuis hier", "Depuis 3 jours", "Depuis plus d'une semaine"),
                )
        }
    }

    private fun iaMessage(
        contenu: String,
        severite: NiveauSeverite? = null,
        suggestions: List<String> = emptyList(),
    ) = TriageMessage(
        id = UUID.randomUUID().toString(),
        role = TriageRole.IA,
        contenu = contenu,
        severite = severite,
        suggestions = suggestions,
    )
}
