package com.clinic.copilot.data.remote

import com.clinic.copilot.data.remote.dto.ConversationDto
import com.clinic.copilot.data.remote.dto.EnvoyerMessageRequest
import com.clinic.copilot.data.remote.dto.MarquerLuResponse
import com.clinic.copilot.data.remote.dto.MessageDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Contrat Retrofit du chat-service, exposé par la gateway sous `/api/chat/`.
 *
 * Le `baseUrl` fourni par le module Hilt se termine par `/api/chat/`, les chemins
 * ci-dessous sont donc relatifs et correspondent aux routes réelles du service :
 * `GET /conversations`, `GET /conversations/{id}/messages`,
 * `POST /conversations/{id}/messages` et `PATCH /conversations/{id}/read`.
 *
 * Le token JWT est ajouté par l'intercepteur OkHttp, jamais dans les arguments.
 */
interface ChatApiService {

    /**
     * Liste les conversations de l'utilisateur authentifié.
     *
     * @return Les conversations du chat-service, déjà triées par date décroissante.
     */
    @GET("conversations")
    suspend fun obtenirConversations(): List<ConversationDto>

    /**
     * Charge une page d'historique, du plus ancien au plus récent.
     *
     * @param conversationId Identifiant de la conversation.
     * @param page Numéro de page basé sur 1.
     * @param limit Taille maximale de la page (100 au plus côté serveur).
     * @return Les messages de la page demandée.
     */
    @GET("conversations/{id}/messages")
    suspend fun obtenirMessages(
        @Path("id") conversationId: String,
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): List<MessageDto>

    /**
     * Envoie un message dans une conversation.
     *
     * @param conversationId Identifiant de la conversation.
     * @param request Corps JSON contenant le texte du message.
     * @return Le message tel que persisté par le serveur.
     */
    @POST("conversations/{id}/messages")
    suspend fun envoyerMessage(
        @Path("id") conversationId: String,
        @Body request: EnvoyerMessageRequest,
    ): MessageDto

    /**
     * Marque comme lus les messages reçus d'une conversation.
     *
     * @param conversationId Identifiant de la conversation.
     * @return Le nombre de messages mis à jour.
     */
    @PATCH("conversations/{id}/read")
    suspend fun marquerCommeLu(@Path("id") conversationId: String): MarquerLuResponse
}
