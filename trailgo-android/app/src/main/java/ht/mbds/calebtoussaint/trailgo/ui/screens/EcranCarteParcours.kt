// ui/screens/EcranCarteParcours.kt
package ht.mbds.calebtoussaint.trailgo.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as ColorAndroid
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import ht.mbds.calebtoussaint.trailgo.data.location.GestionnaireLocalisation
import ht.mbds.calebtoussaint.trailgo.data.model.EtapeResponse
import ht.mbds.calebtoussaint.trailgo.ui.viewmodel.CarteParcoursViewModel
import ht.mbds.calebtoussaint.trailgo.ui.viewmodel.EtatCarteParcours
import ht.mbds.calebtoussaint.trailgo.ui.viewmodel.FabriqueViewModel
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranCarteParcours(
    idParcours: Long,
    surRetour: () -> Unit
) {
    val contexte = LocalContext.current
    val viewModel: CarteParcoursViewModel = viewModel(
        factory = FabriqueViewModel.creerFabriqueCarteParcours(contexte)
    )
    val etat by viewModel.etat.collectAsState()

    val gestionnaireLocalisation = remember { GestionnaireLocalisation(contexte) }
    var permissionRefusee by remember { mutableStateOf(false) }

    val lanceurPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { accordee ->
        if (accordee) {
            permissionRefusee = false
            viewModel.demarrerNavigation()
        } else {
            permissionRefusee = true
        }
    }

    LaunchedEffect(idParcours) {
        viewModel.charger(idParcours)
    }

    // Collecte les positions GPS uniquement pendant la navigation :
    // l'effet est relance a chaque changement de modeNavigation, et
    // Compose annule automatiquement la coroutine precedente, ce qui
    // declenche awaitClose dans GestionnaireLocalisation et coupe le
    // GPS des que la navigation s'arrete.
    LaunchedEffect(etat.modeNavigation) {
        if (etat.modeNavigation) {
            gestionnaireLocalisation.positionsEnFlux().collect { position ->
                viewModel.mettreAJourPosition(position.latitude, position.longitude)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(etat.titre.ifBlank { "Carte du parcours" }, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = surRetour) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                }
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
                            Button(onClick = viewModel::reessayer) { Text("Reessayer") }
                        }
                    }
                }
                else -> {
                    CarteOsm(
                        trace = etat.trace,
                        etapes = etat.etapes,
                        etapesAtteintes = etat.etapesAtteintes,
                        positionUtilisateur = etat.positionUtilisateur,
                        modeNavigation = etat.modeNavigation
                    )

                    if (etat.modeNavigation) {
                        PanneauNavigation(
                            etat = etat,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(16.dp),
                            surArreter = viewModel::arreterNavigation
                        )
                    } else {
                        FloatingActionButton(
                            onClick = {
                                val autorisee = ContextCompat.checkSelfPermission(
                                    contexte, Manifest.permission.ACCESS_FINE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED

                                if (autorisee) {
                                    permissionRefusee = false
                                    viewModel.demarrerNavigation()
                                } else {
                                    lanceurPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp)
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Demarrer la navigation")
                        }

                        if (permissionRefusee) {
                            Card(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Text(
                                    "Autorisation de localisation refusee.",
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
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
private fun PanneauNavigation(
    etat: EtatCarteParcours,
    modifier: Modifier = Modifier,
    surArreter: () -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val prochaine = etat.prochaineEtape

            if (prochaine == null) {
                Text(
                    "Parcours termine !",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    "Prochaine etape : ${prochaine.nom}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        etat.distanceProchaineEtapeM?.let { formaterDistance(it) } ?: "-",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        etat.capProchaineEtapeDeg?.let { formaterDirection(it) } ?: "-",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val progression = if (etat.etapes.isEmpty()) {
                0f
            } else {
                etat.etapesAtteintes.size / etat.etapes.size.toFloat()
            }

            LinearProgressIndicator(
                progress = progression,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "${etat.etapesAtteintes.size} / ${etat.etapes.size} etapes atteintes",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = surArreter,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Arreter la navigation")
            }
        }
    }
}

@Composable
private fun CarteOsm(
    trace: List<GeoPoint>,
    etapes: List<EtapeResponse>,
    etapesAtteintes: Set<Long>,
    positionUtilisateur: GeoPoint?,
    modeNavigation: Boolean
) {
    val contexte = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapView = remember {
        MapView(contexte).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observateur = LifecycleEventObserver { _, evenement ->
            when (evenement) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observateur)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observateur)
            mapView.onDetach()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = Modifier.fillMaxSize(),
        update = { vue ->
            vue.overlays.clear()

            if (trace.isNotEmpty()) {
                val ligne = Polyline().apply {
                    setPoints(trace)
                    outlinePaint.color = ColorAndroid.parseColor("#1976D2")
                    outlinePaint.strokeWidth = 8f
                }
                vue.overlays.add(ligne)
            }

            etapes.forEach { etape ->
                val atteinte = etape.id in etapesAtteintes
                val marqueur = Marker(vue).apply {
                    position = GeoPoint(etape.latitude, etape.longitude)
                    title = if (atteinte) {
                        "✓ ${etape.ordre}. ${etape.nom}"
                    } else {
                        "${etape.ordre}. ${etape.nom}"
                    }
                    snippet = etape.description
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    icon = dessinerMarqueurRond(
                        contexte,
                        if (atteinte) ColorAndroid.parseColor("#2E7D32") else ColorAndroid.parseColor("#D32F2F")
                    )
                }
                vue.overlays.add(marqueur)
            }

            positionUtilisateur?.let { position ->
                val marqueurPosition = Marker(vue).apply {
                    this.position = position
                    title = "Votre position"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    icon = dessinerMarqueurRond(contexte, ColorAndroid.parseColor("#1976D2"))
                }
                vue.overlays.add(marqueurPosition)
            }

            if (modeNavigation && positionUtilisateur != null) {
                // Recentrage automatique sur la position de l'utilisateur
                // pendant la navigation terrain.
                vue.controller.animateTo(positionUtilisateur)
            } else {
                val pointsCadrage = trace.ifEmpty {
                    etapes.map { GeoPoint(it.latitude, it.longitude) }
                }
                if (pointsCadrage.isNotEmpty()) {
                    vue.post {
                        vue.zoomToBoundingBox(
                            BoundingBox.fromGeoPoints(pointsCadrage),
                            true,
                            100
                        )
                    }
                }
            }

            vue.invalidate()
        }
    )
}

/**
 * Genere un marqueur circulaire colore a la volee (bitmap dessine sur
 * un Canvas), plutot que de dependre d'une ressource drawable du
 * systeme dont l'apparence varie selon la version d'Android et le
 * fabricant.
 */
private fun dessinerMarqueurRond(
    contexte: android.content.Context,
    couleurArgb: Int
): BitmapDrawable {
    val tailleDp = 24
    val densite = contexte.resources.displayMetrics.density
    val tailleFinale = (tailleDp * densite).toInt()

    val bitmap = Bitmap.createBitmap(tailleFinale, tailleFinale, Bitmap.Config.ARGB_8888)
    val canevas = Canvas(bitmap)

    val peintureRemplissage = Paint().apply {
        color = couleurArgb
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    val peintureContour = Paint().apply {
        color = ColorAndroid.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }

    val rayon = tailleFinale / 2f - 2f
    val centre = tailleFinale / 2f
    canevas.drawCircle(centre, centre, rayon, peintureRemplissage)
    canevas.drawCircle(centre, centre, rayon, peintureContour)

    return BitmapDrawable(contexte.resources, bitmap)
}

private fun formaterDistance(metres: Double): String {
    return if (metres < 1000) "${metres.toInt()} m" else "%.1f km".format(metres / 1000)
}

private fun formaterDirection(capDegres: Double): String {
    val directions = listOf(
        "Nord", "Nord-Est", "Est", "Sud-Est",
        "Sud", "Sud-Ouest", "Ouest", "Nord-Ouest"
    )
    val index = ((capDegres + 22.5) / 45.0).toInt() % 8
    return directions[index]
}