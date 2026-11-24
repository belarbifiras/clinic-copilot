package com.clinic.copilot.di

import com.clinic.copilot.data.AppointmentRepository
import com.clinic.copilot.data.AppointmentRepositoryImpl
import com.clinic.copilot.data.FakeAppointmentRepository
import com.clinic.copilot.data.remote.AppointmentApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/**
 * `true` : dépôt en mémoire (parcours démontrable sans serveur).
 * `false` : dépôt Retrofit réel (appointment-service via la gateway).
 *
 * Une seule ligne à modifier pour basculer, tous les ViewModels restent inchangés.
 */
private const val UTILISER_FAKE_REPOSITORY = true

/**
 * Fournit les dépendances propres à la feature rendez-vous.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppointmentModule {

    /**
     * Fournit le contrat Retrofit de l'appointment-service.
     *
     * Le Retrofit injecté par [com.clinic.copilot.core.network.NetworkModule] pointe sur
     * `/api/chat/`. On dérive une instance avec la base `/api/` en réutilisant exactement
     * le même OkHttp, le même Moshi et le même intercepteur JWT : aucun client n'est recréé.
     *
     * @param retrofit Client Retrofit applicatif existant.
     * @return L'interface Retrofit des rendez-vous.
     */
    @Provides
    @Singleton
    fun fournirAppointmentApiService(retrofit: Retrofit): AppointmentApiService =
        retrofit.newBuilder()
            .baseUrl(BASE_URL_API)
            .build()
            .create(AppointmentApiService::class.java)

    /**
     * Sélectionne l'implémentation selon [UTILISER_FAKE_REPOSITORY].
     *
     * @param fake Dépôt de démonstration en mémoire.
     * @param reelle Dépôt branché sur l'API REST.
     * @return Le dépôt injecté dans les ViewModels.
     */
    @Provides
    @Singleton
    fun fournirAppointmentRepository(
        fake: FakeAppointmentRepository,
        reelle: AppointmentRepositoryImpl,
    ): AppointmentRepository = if (UTILISER_FAKE_REPOSITORY) fake else reelle

    private const val BASE_URL_API = "http://10.0.2.2:8080/api/"
}
