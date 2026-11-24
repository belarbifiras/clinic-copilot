package com.clinic.copilot.di

import com.clinic.copilot.data.ChatRepository
import com.clinic.copilot.data.ChatRepositoryImpl
import com.clinic.copilot.data.FakeChatRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * `true` : dépôt en mémoire (démo sans serveur).
 * `false` : dépôt Retrofit réel (chat-service via la gateway).
 *
 * Une seule ligne à modifier pour basculer, tous les ViewModels restent inchangés.
 */
private const val UTILISER_FAKE_REPOSITORY = true

/**
 * Fournit l'implémentation active du dépôt de messagerie.
 */
@Module
@InstallIn(SingletonComponent::class)
object ChatModule {

    /**
     * Sélectionne l'implémentation selon [UTILISER_FAKE_REPOSITORY].
     *
     * @param fake Dépôt de démonstration en mémoire.
     * @param reelle Dépôt branché sur l'API REST.
     * @return Le dépôt injecté dans les ViewModels.
     */
    @Provides
    @Singleton
    fun fournirChatRepository(
        fake: FakeChatRepository,
        reelle: ChatRepositoryImpl,
    ): ChatRepository = if (UTILISER_FAKE_REPOSITORY) fake else reelle
}
