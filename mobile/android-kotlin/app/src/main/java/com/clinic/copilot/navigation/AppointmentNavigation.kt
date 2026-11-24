package com.clinic.copilot.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.clinic.copilot.ui.booking.BookingConfirmationScreen
import com.clinic.copilot.ui.myappointments.MyAppointmentsScreen
import com.clinic.copilot.ui.slots.ParametresConfirmation
import com.clinic.copilot.ui.slots.SlotSelectionScreen

// TODO: provisoire - à rebrancher sur le graphe global (US-05) comme ChatNavigation

/**
 * Graphe local autonome du parcours « Rendez-vous patient » (US-30 à US-35).
 *
 * Conservé pour une utilisation isolée ; dans l'application, les destinations sont
 * imbriquées dans le NavHost de [ChatNavigation] via [appointmentGraph], ce qui permet
 * à l'onglet « RDV » d'y naviguer sans modifier le point d'entrée partagé.
 *
 * @param modifier Modificateur appliqué au conteneur de navigation.
 */
@Composable
fun AppointmentNavigation(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AppointmentRoutes.MES_RENDEZ_VOUS,
        modifier = modifier,
    ) {
        appointmentGraph(navController)
    }
}

/**
 * Déclare les destinations « Mes rendez-vous → créneaux → confirmation » dans un graphe.
 *
 * Cette fonction est partagée entre [AppointmentNavigation] (graphe autonome) et
 * [ChatNavigation] (graphe imbriqué accessible depuis l'onglet RDV), afin d'éviter
 * toute duplication de routes.
 *
 * @param navController Contrôleur du NavHost propriétaire du graphe.
 */
internal fun NavGraphBuilder.appointmentGraph(navController: NavHostController) {
    // L'onglet « RDV » reste actif sur tout le graphe ; un appui ramène à la liste.
    val retourALaListe: () -> Unit = {
        navController.popBackStack(
            route = AppointmentRoutes.MES_RENDEZ_VOUS,
            inclusive = false,
        )
        Unit
    }

    composable(AppointmentRoutes.MES_RENDEZ_VOUS) {
        MyAppointmentsScreen(
            onReprogrammer = { rendezVous ->
                navController.navigate(
                    AppointmentRoutes.creneaux(
                        doctorId = rendezVous.doctorId,
                        doctorName = rendezVous.doctorName,
                        specialite = rendezVous.specialite,
                        appointmentId = rendezVous.id,
                    ),
                )
            },
            onRdvClick = {},
        )
    }

    composable(
        route = AppointmentRoutes.CRENEAUX,
        arguments = listOf(
            navArgument("doctorId") { type = NavType.StringType },
            navArgument("doctorName") { type = NavType.StringType },
            navArgument("specialite") { type = NavType.StringType },
            navArgument("appointmentId") {
                type = NavType.StringType
                defaultValue = ""
            },
            navArgument("date") {
                type = NavType.LongType
                defaultValue = 0L
            },
        ),
    ) {
        SlotSelectionScreen(
            onRetour = { navController.popBackStack() },
            onContinuer = { parametres ->
                navController.navigate(AppointmentRoutes.confirmation(parametres))
            },
            onRdvClick = retourALaListe,
        )
    }

    composable(
        route = AppointmentRoutes.CONFIRMATION,
        arguments = listOf(
            navArgument("doctorId") { type = NavType.StringType },
            navArgument("doctorName") { type = NavType.StringType },
            navArgument("specialite") { type = NavType.StringType },
            navArgument("slotId") { type = NavType.StringType },
            navArgument("date") { type = NavType.LongType },
            navArgument("heureDebut") { type = NavType.LongType },
            navArgument("heureFin") { type = NavType.LongType },
            navArgument("appointmentId") {
                type = NavType.StringType
                defaultValue = ""
            },
        ),
    ) {
        BookingConfirmationScreen(
            onRetour = { navController.popBackStack() },
            onConflit = {
                // Le 409 a déjà déclenché le rechargement des créneaux côté dépôt.
                // On recrée l'écran de sélection pour afficher les données fraîches
                // à la même date, sans laisser d'ancienne occurrence dans la pile.
                val arguments = navController.previousBackStackEntry?.arguments
                navController.popBackStack(AppointmentRoutes.CRENEAUX, inclusive = true)
                navController.navigate(
                    AppointmentRoutes.creneaux(
                        doctorId = arguments?.getString("doctorId").orEmpty(),
                        doctorName = arguments?.getString("doctorName").orEmpty(),
                        specialite = arguments?.getString("specialite").orEmpty(),
                        appointmentId = arguments?.getString("appointmentId").orEmpty(),
                        date = arguments?.getLong("date") ?: 0L,
                    ),
                )
            },
            onTermine = retourALaListe,
            onRdvClick = retourALaListe,
        )
    }
}

/** Routes internes du graphe local de rendez-vous. */
internal object AppointmentRoutes {

    /** Route du graphe imbriqué, cible de l'onglet « RDV » de la messagerie. */
    const val GRAPHE = "appointments"

    /** Destination initiale : liste des rendez-vous. */
    const val MES_RENDEZ_VOUS = "appointments/mes-rendez-vous"

    /** Écran de sélection, avec identifiant médecin et rendez-vous à reprogrammer éventuel. */
    const val CRENEAUX =
        "appointments/creneaux/{doctorId}/{doctorName}/{specialite}?appointmentId={appointmentId}&date={date}"

    /** Écran de confirmation, avec le créneau choisi et les informations d'affichage. */
    const val CONFIRMATION =
        "appointments/confirmation/{doctorId}/{doctorName}/{specialite}/{slotId}/{date}/{heureDebut}/{heureFin}?appointmentId={appointmentId}"

    /**
     * Construit la route de sélection d'un créneau.
     *
     * @param doctorId Identifiant du médecin (par défaut le médecin de démonstration).
     * @param doctorName Nom affiché du médecin.
     * @param specialite Spécialité affichée.
     * @param appointmentId Identifiant du rendez-vous à reprogrammer, ou vide.
     * @param date Jour à présenter, ou 0 pour aujourd'hui.
     */
    fun creneaux(
        doctorId: String = "doctor-marc-laurent",
        doctorName: String = "Dr. Marc Laurent",
        specialite: String = "Cardiologie Générale",
        appointmentId: String = "",
        date: Long = 0L,
    ): String = "appointments/creneaux/${Uri.encode(doctorId)}/${Uri.encode(doctorName)}/" +
        "${Uri.encode(specialite)}?appointmentId=${Uri.encode(appointmentId)}&date=$date"

    /**
     * Construit la route de confirmation à partir des paramètres choisis.
     *
     * @param parametres Médecin, créneau et éventuel rendez-vous à reprogrammer.
     */
    fun confirmation(parametres: ParametresConfirmation): String =
        "appointments/confirmation/${Uri.encode(parametres.doctorId)}/" +
            "${Uri.encode(parametres.doctorName)}/${Uri.encode(parametres.specialite)}/" +
            "${Uri.encode(parametres.slotId)}/${parametres.date}/" +
            "${parametres.heureDebut}/${parametres.heureFin}" +
            "?appointmentId=${Uri.encode(parametres.appointmentId)}"
}
