// ui/screens/EcranDetailParcours.kt
package ht.mbds.calebtoussaint.trailgo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import ht.mbds.calebtoussaint.trailgo.data.api.ApiClient
import ht.mbds.calebtoussaint.trailgo.data.model.EtapeResponse
import ht.mbds.calebtoussaint.trailgo.data.model.ParcoursResponse
import ht.mbds.calebtoussaint.trailgo.ui.components.ArrierePlanTraces
import ht.mbds.calebtoussaint.trailgo.ui.viewmodel.DetailParcoursViewModel
import ht.mbds.calebtoussaint.trailgo.ui.viewmodel.FabriqueViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranDetailParcours(
    idParcours: Long,
    surRetour: () -> Unit
) {
    val contexte = LocalContext.current
    val viewModel: DetailParcoursViewModel = viewModel(
        factory = FabriqueViewModel.creerFabriqueDetailParcours(contexte)
    )
    val etat by viewModel.etat.collectAsState()

    // Declenche le chargement une seule fois, et le relance uniquement
    // si l'identifiant change. C'est l'equivalent Compose du "init {}"
    // utilise dans ListeParcoursViewModel.
    LaunchedEffect(idParcours) {
        viewModel.charger(idParcours)
    }

    Box(modifier = Modifier.fillMaxSize()) {

        ArrierePlanTraces()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            etat.parcours?.titre ?: "Parcours",
                            maxLines = 1
                        )
                    },
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

            Box(modifier = Modifier
                .fillMaxSize()
                .padding(paddingInterieur)
            ) {
                when {
                    etat.chargement -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    etat.erreur != null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(etat.erreur!!, color = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = viewModel::reessayer) { Text("Reessayer") }
                            }
                        }
                    }

                    etat.parcours != null -> {
                        ContenuDetail(parcours = etat.parcours!!)
                    }
                }
            }
        }
    }
}

@Composable
private fun ContenuDetail(parcours: ParcoursResponse) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            CarteEnTete(parcours)
        }

        item {
            CarteStatistiques(parcours)
        }

        item {
            Text(
                "Etapes du parcours (${parcours.etapes.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (parcours.etapes.isEmpty()) {
            item {
                Text(
                    "Aucune etape n'a encore ete definie pour ce parcours.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            // Les etapes sont triees par "ordre" cote client : on ne
            // depend pas de l'ordre de serialisation renvoye par l'API.
            items(parcours.etapes.sortedBy { it.ordre }, key = { it.id }) { etape ->
                CarteEtape(etape)
            }
        }
    }
}

@Composable
private fun CarteEnTete(parcours: ParcoursResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            if (parcours.imageCouverture != null) {
                AsyncImage(
                    model = ApiClient.urlAbsolueImage(parcours.imageCouverture),
                    contentDescription = parcours.titre,
                    // Crop evite la deformation quand le ratio de l'image
                    // ne correspond pas a celui du cadre.
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    parcours.titre,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = {}, label = { Text(parcours.theme) })
                    AssistChip(onClick = {}, label = { Text(parcours.difficulte) })
                }

                if (parcours.zoneNom != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Zone : ${parcours.zoneNom}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!parcours.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        parcours.description,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun CarteStatistiques(parcours: ParcoursResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Statistique(
                valeur = parcours.distanceTotaleKm?.let { "%.1f km".format(it) } ?: "-",
                libelle = "Distance"
            )
            Statistique(
                valeur = formaterDuree(parcours.dureeEstimeeMin),
                libelle = "Duree"
            )
            Statistique(
                valeur = parcours.etapes.size.toString(),
                libelle = "Etapes"
            )
            Statistique(
                valeur = parcours.nbConsultations.toString(),
                libelle = "Vues"
            )
        }
    }
}

@Composable
private fun Statistique(valeur: String, libelle: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            valeur,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            libelle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CarteEtape(etape: EtapeResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp)) {

            // Pastille numerotee, reprise visuelle des marqueurs de la
            // carte Leaflet du back office.
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    etape.ordre.toString(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    etape.nom,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                if (!etape.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        etape.description,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (etape.photo != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    AsyncImage(
                        model = ApiClient.urlAbsolueImage(etape.photo),
                        contentDescription = etape.nom,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                val details = buildList {
                    etape.dureeVisiteMin?.let { add("$it min sur place") }
                    add("%.4f, %.4f".format(etape.latitude, etape.longitude))
                }
                Text(
                    details.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 95 minutes -> "1 h 35". Plus lisible qu'un nombre de minutes brut
 * des que la duree depasse l'heure.
 */
private fun formaterDuree(minutes: Int?): String {
    if (minutes == null) return "-"
    if (minutes < 60) return "$minutes min"
    val heures = minutes / 60
    val reste = minutes % 60
    return if (reste == 0) "$heures h" else "$heures h $reste"
}