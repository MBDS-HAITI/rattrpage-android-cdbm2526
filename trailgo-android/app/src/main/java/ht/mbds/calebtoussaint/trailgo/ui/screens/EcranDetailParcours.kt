// ui/screens/EcranDetailParcours.kt
package ht.mbds.calebtoussaint.trailgo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import ht.mbds.calebtoussaint.trailgo.data.api.ApiClient
import ht.mbds.calebtoussaint.trailgo.data.model.AvisResponse
import ht.mbds.calebtoussaint.trailgo.data.model.EtapeResponse
import ht.mbds.calebtoussaint.trailgo.data.model.ParcoursResponse
import ht.mbds.calebtoussaint.trailgo.ui.components.ArrierePlanTraces
import ht.mbds.calebtoussaint.trailgo.ui.viewmodel.AvisViewModel
import ht.mbds.calebtoussaint.trailgo.ui.viewmodel.DetailParcoursViewModel
import ht.mbds.calebtoussaint.trailgo.ui.viewmodel.EtatAvis
import ht.mbds.calebtoussaint.trailgo.ui.viewmodel.FabriqueViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranDetailParcours(
    idParcours: Long,
    surRetour: () -> Unit,
    surVoirCarte: () -> Unit
) {
    val contexte = LocalContext.current

    val viewModel: DetailParcoursViewModel = viewModel(
        factory = FabriqueViewModel.creerFabriqueDetailParcours(contexte)
    )
    val etat by viewModel.etat.collectAsState()

    val avisViewModel: AvisViewModel = viewModel(
        factory = FabriqueViewModel.creerFabriqueAvis(contexte)
    )
    val etatAvis by avisViewModel.etat.collectAsState()

    LaunchedEffect(idParcours) {
        viewModel.charger(idParcours)
        avisViewModel.charger(idParcours)
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
                    actions = {
                        if (etat.parcours != null) {
                            IconButton(
                                onClick = viewModel::togglerFavori,
                                enabled = !etat.chargementFavori
                            ) {
                                Icon(
                                    imageVector = if (etat.estFavori) {
                                        Icons.Filled.Favorite
                                    } else {
                                        Icons.Filled.FavoriteBorder
                                    },
                                    contentDescription = if (etat.estFavori) {
                                        "Retirer des favoris"
                                    } else {
                                        "Ajouter aux favoris"
                                    },
                                    tint = if (etat.estFavori) Color.Red else LocalContentColor.current
                                )
                            }
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
                        ContenuDetail(
                            parcours = etat.parcours!!,
                            surVoirCarte = surVoirCarte,
                            etatAvis = etatAvis,
                            surOuvrirFormulaireAvis = avisViewModel::ouvrirFormulaire
                        )

                        if (etatAvis.formulaireOuvert) {
                            DialogueDepotAvis(
                                etatAvis = etatAvis,
                                surFermer = avisViewModel::fermerFormulaire,
                                surModifierNote = avisViewModel::modifierNoteSaisie,
                                surModifierCommentaire = avisViewModel::modifierCommentaireSaisi,
                                surSoumettre = avisViewModel::soumettre
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContenuDetail(
    parcours: ParcoursResponse,
    surVoirCarte: () -> Unit,
    etatAvis: EtatAvis,
    surOuvrirFormulaireAvis: () -> Unit
) {
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
            Button(
                onClick = surVoirCarte,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Voir la carte du parcours")
            }
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
            items(parcours.etapes.sortedBy { it.ordre }, key = { it.id }) { etape ->
                CarteEtape(etape)
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Avis (${etatAvis.avis.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = surOuvrirFormulaireAvis) {
                    Text("Deposer un avis")
                }
            }
        }

        if (etatAvis.avis.isEmpty()) {
            item {
                Text(
                    "Aucun avis pour le moment. Soyez le premier a en deposer un.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            items(etatAvis.avis, key = { it.id }) { avis ->
                CarteAvis(avis)
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

@Composable
private fun CarteAvis(avis: AvisResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(avis.auteurNom, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    // Les 10 premiers caracteres d'une date ISO donnent
                    // directement AAAA-MM-JJ, sans avoir besoin d'une
                    // bibliotheque de formatage de dates.
                    avis.dateCreation.take(10),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            EtoilesLectureSeule(note = avis.note)

            if (!avis.commentaire.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(avis.commentaire, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun EtoilesLectureSeule(note: Int) {
    Row {
        for (i in 1..5) {
            Text(
                text = if (i <= note) "★" else "☆",
                fontSize = 16.sp,
                color = if (i <= note) Color(0xFFFFA000) else Color.Gray
            )
        }
    }
}

@Composable
private fun DialogueDepotAvis(
    etatAvis: EtatAvis,
    surFermer: () -> Unit,
    surModifierNote: (Int) -> Unit,
    surModifierCommentaire: (String) -> Unit,
    surSoumettre: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!etatAvis.envoiEnCours) surFermer() },
        title = { Text("Deposer un avis") },
        text = {
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    for (i in 1..5) {
                        Text(
                            text = if (i <= etatAvis.noteSaisie) "★" else "☆",
                            fontSize = 32.sp,
                            color = if (i <= etatAvis.noteSaisie) Color(0xFFFFA000) else Color.Gray,
                            modifier = Modifier
                                .clickable(enabled = !etatAvis.envoiEnCours) { surModifierNote(i) }
                                .padding(horizontal = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = etatAvis.commentaireSaisi,
                    onValueChange = surModifierCommentaire,
                    label = { Text("Commentaire (optionnel)") },
                    enabled = !etatAvis.envoiEnCours,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                etatAvis.erreurEnvoi?.let { message ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = surSoumettre, enabled = !etatAvis.envoiEnCours) {
                if (etatAvis.envoiEnCours) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Envoyer")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = surFermer, enabled = !etatAvis.envoiEnCours) {
                Text("Annuler")
            }
        }
    )
}

private fun formaterDuree(minutes: Int?): String {
    if (minutes == null) return "-"
    if (minutes < 60) return "$minutes min"
    val heures = minutes / 60
    val reste = minutes % 60
    return if (reste == 0) "$heures h" else "$heures h $reste"
}