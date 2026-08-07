// ui/screens/EcranCarteParcours.kt
package ht.mbds.calebtoussaint.trailgo.ui.screens

import android.graphics.Color as ColorAndroid
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import ht.mbds.calebtoussaint.trailgo.data.model.EtapeResponse
import ht.mbds.calebtoussaint.trailgo.ui.viewmodel.CarteParcoursViewModel
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

    LaunchedEffect(idParcours) {
        viewModel.charger(idParcours)
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
                    CarteOsm(trace = etat.trace, etapes = etat.etapes)
                }
            }
        }
    }
}

@Composable
private fun CarteOsm(
    trace: List<GeoPoint>,
    etapes: List<EtapeResponse>
) {
    val contexte = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapView = remember {
        MapView(contexte).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
        }
    }

    // Osmdroid recommande de suivre le cycle de vie de l'ecran pour
    // liberer les ressources reseau des tuiles quand il n'est pas visible.
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
                val marqueur = Marker(vue).apply {
                    position = GeoPoint(etape.latitude, etape.longitude)
                    title = "${etape.ordre}. ${etape.nom}"
                    snippet = etape.description
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                vue.overlays.add(marqueur)
            }

            // Cadre la carte sur le trace, ou a defaut sur les etapes,
            // pour que tout soit visible sans zoom manuel a l'ouverture.
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

            vue.invalidate()
        }
    )
}