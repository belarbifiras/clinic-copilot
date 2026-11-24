package com.clinic.copilot.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.clinic.copilot.feature.appointments.ui.AppointmentsScreen
import com.clinic.copilot.feature.auth.ui.AuthNavigation
import com.clinic.copilot.feature.doctors.ui.DoctorsScreen
import com.clinic.copilot.feature.home.ui.HomeScreen
import com.clinic.copilot.feature.profile.ui.ProfileScreen
import com.clinic.copilot.feature.triage.ui.TriageScreen
import com.clinic.copilot.ui.chat.ChatScreen
import com.clinic.copilot.ui.conversations.ConversationListScreen

/**
 * Routes du graphe global de l'application Clinic Copilot.
 */
object AppRoutes {
    const val AUTH = "app/auth"
    const val HOME = "app/home"
    const val TRIAGE = "app/triage"
    const val DOCTORS = "app/doctors"
    const val APPOINTMENTS = "app/appointments"
    const val MESSAGES = "app/messages"
    const val CHAT = "app/chat/{conversationId}/{nomContact}/{currentUserId}"
    const val PROFILE = "app/profile"

    fun chat(conversationId: String, nomContact: String, currentUserId: String) =
        "app/chat/${Uri.encode(conversationId)}/${Uri.encode(nomContact)}/${Uri.encode(currentUserId)}"
}

/**
 * Graphe de navigation unifié de Clinic Copilot.
 *
 * Orchestre les 5 briques fonctionnelles principales :
 * 1. Authentification (Membre 1 & Membre 2 : US-01, US-10, US-11)
 * 2. Accueil & Triage IA (Membre 2 : US-12, US-70)
 * 3. Annuaire & Praticiens (Membre 3 : US-20, US-22)
 * 4. Dossier Médical & RDV (Membre 4 : US-30, US-32)
 * 5. Messagerie médicale & Chat (Membre 5 / 3 : US-50, US-51)
 * 6. Profil Patient HDS & Déconnexion (Membre 2 : US-13)
 */
@Composable
fun AppNavGraph(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AppRoutes.AUTH,
        modifier = modifier,
    ) {
        // ── 1. Authentification (Membre 1) ─────────────────────────────────
        composable(AppRoutes.AUTH) {
            AuthNavigation(
                onAuthentifie = { userId ->
                    navController.navigate(AppRoutes.HOME) {
                        popUpTo(AppRoutes.AUTH) { inclusive = true }
                    }
                },
            )
        }

        // ── 2. Accueil (Membre 2) ─────────────────────────────────────────
        composable(AppRoutes.HOME) {
            HomeScreen(
                onAllerTriage = { navController.navigate(AppRoutes.TRIAGE) },
                onAllerMessages = { navController.navigate(AppRoutes.MESSAGES) },
                onAllerMedecins = { navController.navigate(AppRoutes.DOCTORS) },
                onAllerRdv = { navController.navigate(AppRoutes.APPOINTMENTS) },
                onAllerProfil = { navController.navigate(AppRoutes.PROFILE) },
            )
        }

        // ── 3. Triage IA (Membre 2) ───────────────────────────────────────
        composable(AppRoutes.TRIAGE) {
            TriageScreen(onRetour = { navController.popBackStack() })
        }

        // ── 4. Annuaire Médecins (Membre 3) ───────────────────────────────
        composable(AppRoutes.DOCTORS) {
            DoctorsScreen(
                onDoctorClick = { doc ->
                    navController.navigate(
                        AppRoutes.chat(
                            conversationId = "conv-${doc.id}",
                            nomContact = doc.nomComplet,
                            currentUserId = "patient-demo",
                        ),
                    )
                },
                onAccueil = { navController.navigate(AppRoutes.HOME) },
                onRdv = { navController.navigate(AppRoutes.APPOINTMENTS) },
                onMessages = { navController.navigate(AppRoutes.MESSAGES) },
                onProfil = { navController.navigate(AppRoutes.PROFILE) },
            )
        }

        // ── 5. Dossier Médical & RDV (Membre 4) ───────────────────────────
        composable(AppRoutes.APPOINTMENTS) {
            AppointmentsScreen(
                onAccueil = { navController.navigate(AppRoutes.HOME) },
                onMedecins = { navController.navigate(AppRoutes.DOCTORS) },
                onMessages = { navController.navigate(AppRoutes.MESSAGES) },
                onProfil = { navController.navigate(AppRoutes.PROFILE) },
            )
        }

        // ── 6. Messagerie - Liste conversations (Membre 5 / 3) ────────────
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

        // ── 7. Messagerie - Chat individuel (Membre 5 / 3) ─────────────────
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

        // ── 8. Profil Patient & Paramètres (Membre 2) ─────────────────────
        composable(AppRoutes.PROFILE) {
            ProfileScreen(
                onDeconnexion = {
                    navController.navigate(AppRoutes.AUTH) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onAccueil = { navController.navigate(AppRoutes.HOME) },
                onMedecins = { navController.navigate(AppRoutes.DOCTORS) },
                onRdv = { navController.navigate(AppRoutes.APPOINTMENTS) },
                onMessages = { navController.navigate(AppRoutes.MESSAGES) },
            )
        }
    }
}
