package com.clinic.copilot.feature.auth.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

/** Routes internes du graphe d'authentification. */
private object AuthRoutes {
    const val LOGIN = "auth/login"
    const val REGISTER = "auth/register"
}

/**
 * Graphe de navigation local pour l'authentification.
 *
 * Il gère le aller-retour entre [LoginScreen] et [RegisterScreen].
 * Dès qu'une des deux pages confirme la connexion, [onAuthentifie] est appelé
 * avec le userId pour basculer vers le graphe principal.
 *
 * @param onAuthentifie Callback déclenché après connexion ou inscription réussie.
 * @param modifier      Modificateur appliqué au NavHost.
 */
@Composable
fun AuthNavigation(
    onAuthentifie: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AuthRoutes.LOGIN,
        modifier = modifier,
    ) {
        composable(AuthRoutes.LOGIN) {
            LoginScreen(
                onConnecte = onAuthentifie,
                onAllerInscription = { navController.navigate(AuthRoutes.REGISTER) },
            )
        }
        composable(AuthRoutes.REGISTER) {
            RegisterScreen(
                onInscrit = onAuthentifie,
                onAllerConnexion = { navController.popBackStack() },
            )
        }
    }
}
