// MainActivity.kt
package ht.mbds.calebtoussaint.trailgo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import ht.mbds.calebtoussaint.trailgo.ui.screens.EcranConnexion
import ht.mbds.calebtoussaint.trailgo.ui.theme.TrailGoTheme

/**
 * Point d'entree de l'application, l'equivalent de main.jsx cote React.
 *
 * Pour l'instant elle affiche uniquement l'ecran de connexion. La
 * navigation complete entre plusieurs ecrans (Navigation Component)
 * arrive a l'etape suivante : ce fichier deviendra alors tres court,
 * se contentant de lancer le systeme de navigation.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TrailGoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { paddingInterieur ->
                    // Etat temporaire, uniquement pour verifier que le
                    // cycle de connexion fonctionne de bout en bout
                    // avant de construire la vraie navigation.
                    var estConnecte by remember { mutableStateOf(false) }

                    if (estConnecte) {
                        EcranPlaceholderConnecte(
                            modifier = Modifier.padding(paddingInterieur)
                        )
                    } else {
                        EcranConnexion(
                            surConnexionReussie = { estConnecte = true }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Ecran temporaire, juste pour confirmer visuellement qu'on a bien
 * franchi l'etape de connexion. Sera remplace par la vraie liste des
 * parcours des que la navigation sera en place.
 */
@androidx.compose.runtime.Composable
private fun EcranPlaceholderConnecte(modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        androidx.compose.material3.Text("Connexion reussie ! La liste des parcours arrive a l'etape suivante.")
    }
}