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
import ht.mbds.calebtoussaint.trailgo.data.api.GestionnaireJeton
import ht.mbds.calebtoussaint.trailgo.navigation.Routes
import ht.mbds.calebtoussaint.trailgo.ui.screens.EcranCarteParcours
import ht.mbds.calebtoussaint.trailgo.ui.screens.EcranConnexion
import ht.mbds.calebtoussaint.trailgo.ui.screens.EcranDetailParcours
import ht.mbds.calebtoussaint.trailgo.ui.screens.EcranFavoris
import ht.mbds.calebtoussaint.trailgo.ui.screens.EcranListeParcours
import ht.mbds.calebtoussaint.trailgo.ui.theme.TrailGoTheme
import org.osmdroid.config.Configuration
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidBasePath = File(cacheDir, "osmdroid")
            osmdroidTileCache = File(osmdroidBasePath, "tiles")
        }

        val dejaConnecte = GestionnaireJeton(applicationContext).estConnecte()
        val destinationDepart = if (dejaConnecte) {
            Routes.LISTE_PARCOURS
        } else {
            Routes.CONNEXION
        }

        enableEdgeToEdge()
        setContent {
            TrailGoTheme {
                ApplicationTrailGo(destinationDepart = destinationDepart)
            }
        }
        }
    }

@Composable
fun ApplicationTrailGo(destinationDepart: String) {
    val controleurNavigation = rememberNavController()

    NavHost(
        navController = controleurNavigation,
        startDestination = destinationDepart
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
                },
                surVoirFavoris = {
                    controleurNavigation.navigate(Routes.FAVORIS)
                }
            )
        }

        composable(Routes.FAVORIS) {
            EcranFavoris(
                surRetour = { controleurNavigation.popBackStack() },
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