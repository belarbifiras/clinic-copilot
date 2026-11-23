package com.clinic.copilot.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.clinic.copilot.feature.auth.ui.AuthNavigation
import com.clinic.copilot.feature.home.ui.HomeScreen
import com.clinic.copilot.feature.triage.ui.TriageScreen
import com.clinic.copilot.ui.chat.ChatScreen
import com.clinic.copilot.ui.conversations.ConversationListScreen

/**
 * Routes du graphe global de l'application.
 *
 * AUTH     → géré par un sous-graphe dans AuthNavigation.
 * HOME     → écran d'accueil (Membre 2).
 * TRIAGE   → triage IA (Membre 2).
 * MESSAGES → liste des conversations (Membre 3).
 * CHAT     → discussion individuelle (Membre 3).
 */
private object AppRoutes {
    const val AUTH = "app/auth"
    const val HOME = "app/home"
    const val TRIAGE = "app/triage"
    const val MESSAGES = "app/messages"
    const val CHAT = "app/chat/{conversationId}/{nomContact}/{currentUserId}"

    fun chat(conversationId: String, nomContact: String, currentUserId: String) =
        "app/chat/${Uri.encode(conversationId)}/${Uri.encode(nomContact)}/${Uri.encode(currentUserId)}"
}

/**
 * Graphe de navigation global.
 *
 * Le point de départ est AUTH ; dès l'authentification réussie,
 * l'utilisateur est redirigé vers HOME (popUpTo AUTH inclusive pour
 * éviter de pouvoir revenir à l'écran de connexion via le bouton Retour).
 *
 * @param modifier Modificateur appliqué au NavHost.
 */
@Composable
fun AppNavGraph(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AppRoutes.AUTH,
        modifier = modifier,
    ) {
        // ── Authentification (Membre 1) ────────────────────────────────────
        composable(AppRoutes.AUTH) {
            AuthNavigation(
                onAuthentifie = { userId ->
                    navController.navigate(AppRoutes.HOME) {
                        popUpTo(AppRoutes.AUTH) { inclusive = true }
                    }
                },
            )
        }

        // ── Accueil (Membre 2) ────────────────────────────────────────────
        composable(AppRoutes.HOME) {
            HomeScreen(
                onAllerTriage = { navController.navigate(AppRoutes.TRIAGE) },
                onAllerMessages = { navController.navigate(AppRoutes.MESSAGES) },
            )
        }

        // ── Triage IA (Membre 2) ──────────────────────────────────────────
        composable(AppRoutes.TRIAGE) {
            TriageScreen(onRetour = { navController.popBackStack() })
        }

        // ── Liste des conversations (Membre 3) ────────────────────────────
        composable(AppRoutes.MESSAGES) {
            ConversationListScreen(
                onConversationClick = { conversation ->
                    navController.navigate(
                        AppRoutes.chat(
                            conversationId = conversation.id,
                            nomContact = conversation.nomContact,
                            currentUserId = "patient-demo",
                        ),
                    )
                },
            )
        }

        // ── Discussion (Membre 3) ─────────────────────────────────────────
        composable(
            route = AppRoutes.CHAT,
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
