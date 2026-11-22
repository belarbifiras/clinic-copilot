package com.clinic.copilot.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.clinic.copilot.ui.chat.ChatScreen
import com.clinic.copilot.ui.conversations.ConversationListScreen

// TODO: provisoire - à rebrancher sur le graphe global (US-05) si besoin

/**
 * Graphe local minimal de la messagerie : liste des conversations puis écran de chat.
 *
 * Les trois paramètres (identifiant, nom du contact et utilisateur courant) sont transportés
 * dans la route puis lus par le [androidx.lifecycle.SavedStateHandle] du ChatViewModel.
 *
 * @param currentUserId Identifiant utilisateur fourni par la session appelante.
 * @param modifier Modificateur appliqué au conteneur de navigation.
 */
@Composable
fun ChatNavigation(
    currentUserId: String,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = ChatRoutes.CONVERSATIONS,
        modifier = modifier,
    ) {
        composable(ChatRoutes.CONVERSATIONS) {
            ConversationListScreen(
                onConversationClick = { conversation ->
                    navController.navigate(
                        ChatRoutes.discussion(
                            conversationId = conversation.id,
                            nomContact = conversation.nomContact,
                            currentUserId = currentUserId,
                        ),
                    )
                },
            )
        }
        composable(
            route = ChatRoutes.CHAT,
            arguments = listOf(
                navArgument("conversationId") { type = NavType.StringType },
                navArgument("nomContact") { type = NavType.StringType },
                navArgument("currentUserId") { type = NavType.StringType },
            ),
        ) {
            ChatScreen(onRetour = { navController.popBackStack() })
        }
    }
}

/** Routes internes du graphe local de messagerie. */
private object ChatRoutes {

    /** Destination initiale : liste des conversations. */
    const val CONVERSATIONS = "chat/conversations"

    /** Route du chat avec ses trois arguments nommés. */
    const val CHAT = "chat/discussion/{conversationId}/{nomContact}/{currentUserId}"

    /**
     * Construit la route d'une discussion en encodant les valeurs texte.
     *
     * @param conversationId Identifiant de la conversation.
     * @param nomContact Nom affiché du contact.
     * @param currentUserId Identifiant de l'utilisateur courant.
     */
    fun discussion(conversationId: String, nomContact: String, currentUserId: String): String =
        "chat/discussion/${Uri.encode(conversationId)}/${Uri.encode(nomContact)}/${Uri.encode(currentUserId)}"
}
