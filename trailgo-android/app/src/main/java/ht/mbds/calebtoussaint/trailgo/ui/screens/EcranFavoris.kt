// ui/screens/EcranFavoris.kt
package ht.mbds.calebtoussaint.trailgo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
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
import ht.mbds.calebtoussaint.trailgo.data.model.FavoriResponse
import ht.mbds.calebtoussaint.trailgo.ui.components.ArrierePlanTraces
import ht.mbds.calebtoussaint.trailgo.ui.viewmodel.FabriqueViewModel
import ht.mbds.calebtoussaint.trailgo.ui.viewmodel.FavorisViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranFavoris(
    surRetour: () -> Unit,
    surParcoursClique: (Long) -> Unit
) {
    val contexte = LocalContext.current
    val viewModel: FavorisViewModel = viewModel(
        factory = FabriqueViewModel.creerFabriqueFavoris(contexte)
    )
    val etat by viewModel.etat.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {

        ArrierePlanTraces()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Mes favoris") },
                    navigationIcon = {
                        IconButton(onClick = surRetour) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Retour"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { paddingInterieur ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingInterieur)
            ) {
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
                    etat.favoris.isEmpty() -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Aucun parcours en favori pour le moment.")
                        }
                    }
                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(etat.favoris, key = { it.parcours.id }) { favori ->
                                CarteFavori(
                                    favori = favori,
                                    onClic = { surParcoursClique(favori.parcours.id) },
                                    onRetirer = { viewModel.retirer(favori.parcours.id) }
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
private fun CarteFavori(
    favori: FavoriResponse,
    onClic: () -> Unit,
    onRetirer: () -> Unit
) {
    Card(
        onClick = onClic,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            if (favori.parcours.imageCouverture != null) {
                AsyncImage(
                    model = ApiClient.urlAbsolueImage(favori.parcours.imageCouverture),
                    contentDescription = favori.parcours.titre,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                )
            }

            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(favori.parcours.titre, style = MaterialTheme.typography.titleMedium)

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(onClick = {}, label = { Text(favori.parcours.theme) })
                        AssistChip(onClick = {}, label = { Text(favori.parcours.difficulte) })
                    }
                }

                IconButton(onClick = onRetirer) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Retirer des favoris"
                    )
                }
            }
        }
    }
}