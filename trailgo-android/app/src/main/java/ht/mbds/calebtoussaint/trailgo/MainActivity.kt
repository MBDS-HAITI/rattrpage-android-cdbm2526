// MainActivity.kt
package ht.mbds.calebtoussaint.trailgo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ht.mbds.calebtoussaint.trailgo.navigation.Routes
import ht.mbds.calebtoussaint.trailgo.ui.screens.EcranConnexion
import ht.mbds.calebtoussaint.trailgo.ui.screens.EcranListeParcours
import ht.mbds.calebtoussaint.trailgo.ui.theme.TrailGoTheme

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
                    // popUpTo + inclusive : retire l'ecran de connexion
                    // de l'historique. Sans cela, le bouton "retour"
                    // ramenerait l'utilisateur connecte vers l'ecran de
                    // connexion, ce qui n'a pas de sens.
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

        composable(Routes.DETAIL_PARCOURS) { backStackEntry ->
            val parcoursId = backStackEntry.arguments
                ?.getString("parcoursId")
                ?.toLongOrNull()

            // Ecran temporaire : la vraie fiche detail arrive a l'etape
            // suivante. On confirme ici seulement que l'identifiant du
            // parcours clique est correctement transmis.
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Detail du parcours n°$parcoursId (a construire)")
            }
        }
    }
}