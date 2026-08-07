// ui/screens/EcranListeParcours.kt
package ht.mbds.calebtoussaint.trailgo.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import ht.mbds.calebtoussaint.trailgo.data.api.ApiClient
import ht.mbds.calebtoussaint.trailgo.data.model.ParcoursSummaryResponse
import ht.mbds.calebtoussaint.trailgo.ui.components.ArrierePlanTraces
import ht.mbds.calebtoussaint.trailgo.ui.viewmodel.FabriqueViewModel
import ht.mbds.calebtoussaint.trailgo.ui.viewmodel.ListeParcoursViewModel

private val THEMES = listOf("CULTUREL", "GASTRONOMIQUE", "NATUREL", "HISTORIQUE")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranListeParcours(
    surParcoursClique: (Long) -> Unit,
    surVoirFavoris: () -> Unit
) {
    val contexte = LocalContext.current
    val viewModel: ListeParcoursViewModel = viewModel(
        factory = FabriqueViewModel.creerFabriqueListeParcours(contexte)
    )
    val etat by viewModel.etat.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {

        ArrierePlanTraces()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Parcours touristiques") },
                    actions = {
                        IconButton(onClick = surVoirFavoris) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = "Mes favoris"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { paddingInterieur ->
            Column(modifier = Modifier.padding(paddingInterieur)) {

                OutlinedTextField(
                    value = etat.recherche,
                    onValueChange = viewModel::changerRecherche,
                    label = { Text("Rechercher un parcours") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    THEMES.forEach { theme ->
                        FilterChip(
                            selected = etat.filtreTheme == theme,
                            onClick = {
                                viewModel.changerFiltreTheme(
                                    if (etat.filtreTheme == theme) null else theme
                                )
                            },
                            label = { Text(theme) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                when {
                    etat.chargement -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    etat.erreur != null -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(etat.erreur!!, color = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = viewModel::charger) { Text("Reessayer") }
                            }
                        }
                    }
                    etat.parcours.isEmpty() -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Aucun parcours ne correspond a ces criteres.")
                        }
                    }
                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(etat.parcours, key = { it.id }) { parcours ->
                                CarteParcours(
                                    parcours = parcours,
                                    onClic = { surParcoursClique(parcours.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CarteParcours(
    parcours: ParcoursSummaryResponse,
    onClic: () -> Unit
) {
    Card(
        onClick = onClic,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            if (parcours.imageCouverture != null) {
                AsyncImage(
                    model = ApiClient.urlAbsolueImage(parcours.imageCouverture),
                    contentDescription = parcours.titre,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                )
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text(parcours.titre, style = MaterialTheme.typography.titleMedium)

                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = {}, label = { Text(parcours.theme) })
                    AssistChip(onClick = {}, label = { Text(parcours.difficulte) })
                }

                Spacer(modifier = Modifier.height(6.dp))

                val infos = buildList {
                    parcours.distanceTotaleKm?.let { add("$it km") }
                    add("${parcours.nbEtapes} etape(s)")
                }
                Text(
                    infos.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}