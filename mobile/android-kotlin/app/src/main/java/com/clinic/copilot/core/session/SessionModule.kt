package com.clinic.copilot.core.session

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Fournit la session courante à l'ensemble de l'application.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SessionModule {

    /**
     * Lie l'interface de session à son implémentation de démonstration.
     *
     * @param provider Fournisseur en mémoire injecté en singleton.
     * @return Le fournisseur exposé au client réseau et aux dépôts.
     */
    @Binds
    @Singleton
    abstract fun lierTokenProvider(provider: DemoTokenProvider): TokenProvider
}
