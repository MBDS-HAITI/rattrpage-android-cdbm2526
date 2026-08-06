// ui/screens/EcranConnexion.kt
package ht.mbds.calebtoussaint.trailgo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ht.mbds.calebtoussaint.trailgo.ui.viewmodel.ConnexionViewModel
import ht.mbds.calebtoussaint.trailgo.ui.viewmodel.FabriqueViewModel

/**
 * Ecran de connexion.
 *
 * "@Composable" marque une fonction qui decrit une portion d'interface,
 * comme un composant React : elle prend des donnees et renvoie ce qui
 * doit s'afficher. Compose se charge de redessiner uniquement ce qui a
 * change quand l'etat evolue.
 *
 * @param surConnexionReussie callback appele quand l'authentification
 * aboutit ; c'est la navigation (etape suivante) qui decidera de la
 * page suivante, pas cet ecran lui-meme.
 */
@Composable
fun EcranConnexion(
    surConnexionReussie: () -> Unit
) {
    val contexte = androidx.compose.ui.platform.LocalContext.current

    // viewModel() recupere ou cree le ViewModel, en le liant au cycle
    // de vie de l'ecran. La fabrique fournit les dependances (le
    // repository), comme explique dans FabriqueViewModel.kt.
    val viewModel: ConnexionViewModel = viewModel(
        factory = FabriqueViewModel.creerFabriqueConnexion(contexte)
    )

    // collectAsState() transforme le StateFlow du ViewModel en un etat
    // Compose observable : chaque emission declenche une recomposition
    // (un nouveau rendu) de cet ecran, automatiquement.
    val etat by viewModel.etat.collectAsState()

    // Des qu'une connexion reussit, on declenche la navigation.
    LaunchedEffect(etat.connexionReussie) {
        if (etat.connexionReussie) {
            surConnexionReussie()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "TrailGo",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Parcours touristiques",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = etat.email,
                    onValueChange = viewModel::modifierEmail,
                    label = { Text("Email") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = etat.motDePasse,
                    onValueChange = viewModel::modifierMotDePasse,
                    label = { Text("Mot de passe") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                etat.erreur?.let { message ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = viewModel::connecter,
                    enabled = !etat.chargement,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (etat.chargement) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Se connecter")
                    }
                }
            }
        }
    }
}
