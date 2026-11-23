package com.clinic.copilot.feature.auth.di

import com.clinic.copilot.feature.auth.data.FakeAuthRepository
import com.clinic.copilot.feature.auth.domain.AuthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * `true` : dépôt en mémoire (démo sans serveur).
 * `false` : dépôt Retrofit réel (identity-profile-service via la gateway).
 */
private const val UTILISER_FAKE_AUTH = true

/** Fournit l'implémentation active du dépôt d'authentification. */
@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun fournirAuthRepository(
        fake: FakeAuthRepository,
    ): AuthRepository = if (UTILISER_FAKE_AUTH) fake else error("AuthRepositoryImpl non implémenté")
}
