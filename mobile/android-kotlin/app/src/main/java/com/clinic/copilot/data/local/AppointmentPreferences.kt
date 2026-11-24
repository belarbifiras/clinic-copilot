package com.clinic.copilot.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Stockage DataStore unique de la feature rendez-vous. */
private val Context.appointmentPreferencesStore by preferencesDataStore(name = "appointment_preferences")

/**
 * Préférences persistantes des rendez-vous, exposées sous forme de Flow.
 *
 * @param context Contexte application, conservé aussi longtemps que le singleton.
 */
@Singleton
class AppointmentPreferences @Inject constructor(
    @ApplicationContext context: Context,
) {

    private val dataStore = context.appointmentPreferencesStore

    /** Nom de l'onglet consulté en dernier ; `A_VENIR` au premier lancement. */
    val dernierOngletConsulte: Flow<String> = dataStore.data
        .catch { cause ->
            if (cause is IOException) emit(emptyPreferences()) else throw cause
        }
        .map { preferences -> preferences[KEY_DERNIER_ONGLET] ?: ONGLET_PAR_DEFAUT }

    /** Active ou désactive les rappels de rendez-vous ; `true` par défaut. */
    val rappelsActifs: Flow<Boolean> = dataStore.data
        .catch { cause ->
            if (cause is IOException) emit(emptyPreferences()) else throw cause
        }
        .map { preferences -> preferences[KEY_RAPPELS_ACTIFS] ?: true }

    /**
     * Mémorise l'onglet ouvert pour la prochaine session.
     *
     * @param onglet Nom de l'onglet (`A_VENIR` ou `PASSES`).
     */
    suspend fun enregistrerDernierOnglet(onglet: String) {
        dataStore.edit { preferences -> preferences[KEY_DERNIER_ONGLET] = onglet }
    }

    /**
     * Active ou désactive les rappels.
     *
     * @param actifs Nouvelle valeur, persistée immédiatement sur disque.
     */
    suspend fun setRappelsActifs(actifs: Boolean) {
        dataStore.edit { preferences -> preferences[KEY_RAPPELS_ACTIFS] = actifs }
    }

    private companion object {
        const val ONGLET_PAR_DEFAUT = "A_VENIR"
        val KEY_DERNIER_ONGLET = stringPreferencesKey("dernier_onglet_rendez_vous")
        val KEY_RAPPELS_ACTIFS = booleanPreferencesKey("rappels_actifs")
    }
}
