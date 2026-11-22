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

/** Stockage DataStore unique de la feature messagerie. */
private val Context.chatPreferencesStore by preferencesDataStore(name = "chat_preferences")

/**
 * Préférences persistantes de la messagerie, exposées sous forme de Flow.
 *
 * Le fichier DataStore survit à la fermeture de l'application : les deux valeurs
 * sont retrouvées intactes au prochain lancement.
 *
 * @param context Contexte application, conservé aussi longtemps que le singleton.
 */
@Singleton
class ChatPreferences @Inject constructor(
    @ApplicationContext context: Context,
) {

    private val dataStore = context.chatPreferencesStore

    /** `true` par défaut : l'utilisateur peut basculer ce réglage depuis le menu. */
    val notificationsActivees: Flow<Boolean> = dataStore.data
        .catch { cause ->
            if (cause is IOException) emit(emptyPreferences()) else throw cause
        }
        .map { preferences -> preferences[KEY_NOTIFICATIONS] ?: true }

    /** Identifiant de la dernière conversation ouverte, `null` au premier lancement. */
    val derniereConversationOuverteId: Flow<String?> = dataStore.data
        .catch { cause ->
            if (cause is IOException) emit(emptyPreferences()) else throw cause
        }
        .map { preferences -> preferences[KEY_DERNIERE_CONVERSATION] }

    /**
     * Active ou désactive les notifications de la messagerie.
     *
     * @param active Nouvelle valeur, persistée immédiatement sur disque.
     */
    suspend fun setNotificationsActivees(active: Boolean) {
        dataStore.edit { preferences -> preferences[KEY_NOTIFICATIONS] = active }
    }

    /**
     * Mémorise la conversation ouverte pour la prochaine session.
     *
     * @param conversationId Identifiant à conserver, ou `null` pour effacer la valeur.
     */
    suspend fun enregistrerDerniereConversation(conversationId: String?) {
        dataStore.edit { preferences ->
            if (conversationId.isNullOrBlank()) {
                preferences.remove(KEY_DERNIERE_CONVERSATION)
            } else {
                preferences[KEY_DERNIERE_CONVERSATION] = conversationId
            }
        }
    }

    private companion object {
        val KEY_NOTIFICATIONS = booleanPreferencesKey("notifications_activees")
        val KEY_DERNIERE_CONVERSATION = stringPreferencesKey("derniere_conversation_ouverte_id")
    }
}
