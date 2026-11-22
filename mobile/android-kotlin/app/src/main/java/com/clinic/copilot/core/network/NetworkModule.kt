package com.clinic.copilot.core.network

import com.clinic.copilot.core.session.TokenProvider
import com.clinic.copilot.data.remote.ChatApiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

/**
 * Client réseau unique de l'application : un seul OkHttp, un seul Retrofit,
 * un seul service de chat. Aucun doublon n'est créé dans la feature messagerie.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * Adresse de l'émulateur vers la gateway ; `10.0.2.2` désigne l'hôte depuis Android.
     */
    private const val BASE_URL = "http://10.0.2.2:8080/api/chat/"

    /**
     * Fournit le parseur JSON capable de lire les data classes Kotlin.
     */
    @Provides
    @Singleton
    fun fournirMoshi(): Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    /**
     * Fournit le client OkHttp avec l'intercepteur JWT réutilisé sur tous les appels.
     *
     * @param tokenProvider Source unique du jeton.
     */
    @Provides
    @Singleton
    fun fournirOkHttpClient(tokenProvider: TokenProvider): OkHttpClient {
        // Les corps de messages médicaux ne sont pas journalisés : aucun log de données de santé.
        val journalisation = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.NONE }
        return OkHttpClient.Builder()
            .addInterceptor(AuthorizationInterceptor(tokenProvider))
            .addInterceptor(journalisation)
            .build()
    }

    /**
     * Fournit le client Retrofit branché sur la gateway du chat-service.
     */
    @Provides
    @Singleton
    fun fournirRetrofit(moshi: Moshi, client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    /**
     * Fournit l'interface Retrofit du chat, injectable dans le dépôt réel.
     */
    @Provides
    @Singleton
    fun fournirChatApiService(retrofit: Retrofit): ChatApiService =
        retrofit.create(ChatApiService::class.java)
}

/**
 * Ajoute l'en-tête `Authorization: Bearer <JWT>` à chaque requête.
 *
 * @property tokenProvider Source du jeton, relue à chaque appel pour prendre en charge
 * un renouvellement de session sans recréer le client OkHttp.
 */
internal class AuthorizationInterceptor(
    private val tokenProvider: TokenProvider,
) : Interceptor {

    /**
     * Intercepte la requête et y injecte le jeton courant s'il existe.
     *
     * @param chain Chaîne OkHttp en cours.
     * @return La réponse du serveur.
     */
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenProvider.obtenirToken()
        val requete = if (token.isNullOrBlank()) {
            chain.request()
        } else {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }
        return chain.proceed(requete)
    }
}
