// MainActivity.kt
package ht.mbds.calebtoussaint.trailgo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ht.mbds.calebtoussaint.trailgo.navigation.Routes
import ht.mbds.calebtoussaint.trailgo.ui.screens.EcranCarteParcours
import ht.mbds.calebtoussaint.trailgo.ui.screens.EcranConnexion
import ht.mbds.calebtoussaint.trailgo.ui.screens.EcranDetailParcours
import ht.mbds.calebtoussaint.trailgo.ui.screens.EcranListeParcours
import ht.mbds.calebtoussaint.trailgo.ui.theme.TrailGoTheme
import org.osmdroid.config.Configuration
import java.io.File

/**
 * Point d'entree de l'application.
 *
 * Desormais tres court : elle se contente d'installer le systeme de
 * navigation. Chaque ecran est un composant independant, comme les
 * pages de src/pages/ cote React.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configuration Osmdroid : obligatoire avant tout affichage de
        // carte. Sans un userAgentValue distinct du defaut, les
        // serveurs de tuiles OpenStreetMap refusent les requetes.
        // Le cache est place dans le repertoire prive de l'app, ce qui
        // evite toute demande de permission de stockage.
        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidBasePath = File(cacheDir, "osmdroid")
            osmdroidTileCache = File(osmdroidBasePath, "tiles")
        }

        enableEdgeToEdge()
        setContent {
            TrailGoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
                    ApplicationTrailGo()
                }
            }
        }
    }
}

/**
 * NavHost : le conteneur qui affiche l'ecran correspondant a la route
 * active, avec gestion automatique de l'historique (bouton "retour"
 * systeme). Equivalent de <Routes> + <Route> de React Router.
 */
@Composable
fun ApplicationTrailGo() {
    val controleurNavigation = rememberNavController()

    NavHost(
        navController = controleurNavigation,
        startDestination = Routes.CONNEXION
    ) {
        composable(Routes.CONNEXION) {
            EcranConnexion(
                surConnexionReussie = {
                    controleurNavigation.navigate(Routes.LISTE_PARCOURS) {
                        popUpTo(Routes.CONNEXION) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.LISTE_PARCOURS) {
            EcranListeParcours(
                surParcoursClique = { id ->
                    controleurNavigation.navigate(Routes.detailParcours(id))
                }
            )
        }

        composable(
            route = Routes.DETAIL_PARCOURS,
            arguments = listOf(
                navArgument("parcoursId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val parcoursId = backStackEntry.arguments?.getLong("parcoursId") ?: return@composable

            EcranDetailParcours(
                idParcours = parcoursId,
                surRetour = { controleurNavigation.popBackStack() },
                surVoirCarte = { controleurNavigation.navigate(Routes.carteParcours(parcoursId)) }
            )
        }

        composable(
            route = Routes.CARTE_PARCOURS,
            arguments = listOf(
                navArgument("parcoursId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val parcoursId = backStackEntry.arguments?.getLong("parcoursId") ?: return@composable

            EcranCarteParcours(
                idParcours = parcoursId,
                surRetour = { controleurNavigation.popBackStack() }
            )
        }
    }
}