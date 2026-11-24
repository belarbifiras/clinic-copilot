package com.clinic.copilot.feature.home.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clinic.copilot.core.data.ClinicLocalDatabase
import com.clinic.copilot.core.data.LocalNotification
import com.clinic.copilot.core.data.StatutRdv
import com.clinic.copilot.feature.home.domain.HomeData
import com.clinic.copilot.feature.home.domain.MedicamentJour
import com.clinic.copilot.feature.home.domain.ProchainRdv
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = false,
    val homeData: HomeData? = null,
    val naviguerVersTriage: Boolean = false,
    val notificationsNonLues: Int = 0,
    val notifications: List<LocalNotification> = emptyList(),
    val showNotificationsDialog: Boolean = false,
)

/**
 * ViewModel de l'écran d'accueil – Connecté dynamiquement à [ClinicLocalDatabase].
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val database: ClinicLocalDatabase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                database.currentUser,
                database.rendezVous,
                database.traitements,
                database.notifications,
            ) { user, rdvs, traites, notifs ->
                val prenom = user?.prenom ?: "Patient"

                val prochain = rdvs.firstOrNull { it.statut == StatutRdv.A_VENIR }?.let { rdv ->
                    ProchainRdv(
                        nomMedecin = rdv.nomMedecin,
                        specialite = rdv.specialite,
                        estConfirme = true,
                        dateTexte = rdv.dateTexte,
                        heure = rdv.heure,
                        lieu = rdv.lieu,
                    )
                }

                val meds = traites.map {
                    MedicamentJour(
                        nom = it.nom,
                        posologie = "${it.momentJour.libelle} · ${it.creneauHoraire} (${it.posologie})",
                        pris = it.statutPrise == com.clinic.copilot.feature.appointments.domain.StatutPrise.PRIS,
                    )
                }

                val unreadCount = notifs.count { !it.estLue }

                HomeData(
                    prenomPatient = prenom,
                    prochainRdv = prochain,
                    medicamentsJour = meds,
                    triageSauvegarde = true,
                ) to Pair(unreadCount, notifs)
            }.collect { (homeData, notifsPair) ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        homeData = homeData,
                        notificationsNonLues = notifsPair.first,
                        notifications = notifsPair.second,
                    )
                }
            }
        }
    }

    fun demanderTriage() {
        _uiState.update { it.copy(naviguerVersTriage = true) }
    }

    fun triageNavigueDone() {
        _uiState.update { it.copy(naviguerVersTriage = false) }
    }

    fun validerPriseMedicament(nom: String) {
        val traitement = database.traitements.value.firstOrNull { it.nom == nom }
        if (traitement != null) {
            database.validerPriseMedicament(traitement.id)
        }
    }

    fun toggleNotificationsDialog(afficher: Boolean) {
        _uiState.update { it.copy(showNotificationsDialog = afficher) }
        if (!afficher) {
            database.toutMarquerNotificationsCommeLues()
        }
    }

    fun marquerNotificationLue(id: String) {
        database.marquerNotificationCommeLue(id)
    }
}
