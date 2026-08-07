// ui/components/ArrierePlanTraces.kt
package ht.mbds.calebtoussaint.trailgo.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Fond decoratif : courbes bleues fines sur un degrade clair.
 * Reprend l'identite visuelle du back office React (composant SVG
 * ArrierePlanTraces), pour que les deux interfaces se ressemblent
 * lors de la demonstration.
 *
 * Purement visuel : aucune interaction, aucun etat.
 */

private val BLEU = Color(0xFF2563EB)

@Composable
fun ArrierePlanTraces(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                // Degrade diagonal blanc -> gris tres pale, comme le
                // linear-gradient(160deg, ...) du CSS cote React.
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFFFFFF),
                        Color(0xFFF4F6FB),
                        Color(0xFFEEF1F8)
                    )
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val l = size.width
            val h = size.height

            // Les coordonnees sont exprimees en fractions de l'ecran
            // (0f a 1f) plutot qu'en pixels : le trace s'adapte ainsi
            // a n'importe quelle taille et orientation d'ecran.
            fun courbe(
                depart: Pair<Float, Float>,
                controle1: Pair<Float, Float>,
                controle2: Pair<Float, Float>,
                arrivee: Pair<Float, Float>,
                opacite: Float,
                epaisseur: Float
            ) {
                val chemin = Path().apply {
                    moveTo(depart.first * l, depart.second * h)
                    cubicTo(
                        controle1.first * l, controle1.second * h,
                        controle2.first * l, controle2.second * h,
                        arrivee.first * l, arrivee.second * h
                    )
                }
                drawPath(
                    path = chemin,
                    color = BLEU.copy(alpha = opacite),
                    style = Stroke(width = epaisseur.dp.toPx())
                )
            }

            // Cinq courbes de Bezier cubiques, debordant volontairement
            // des bords (valeurs < 0 ou > 1) pour eviter tout effet de
            // trait qui commencerait ou finirait au milieu de l'ecran.
            courbe(
                depart = -0.1f to 0.78f,
                controle1 = 0.25f to 0.62f,
                controle2 = 0.55f to 0.95f,
                arrivee = 1.1f to 0.70f,
                opacite = 0.16f,
                epaisseur = 1.4f
            )
            courbe(
                depart = -0.1f to 0.55f,
                controle1 = 0.30f to 0.85f,
                controle2 = 0.65f to 0.60f,
                arrivee = 1.1f to 0.80f,
                opacite = 0.13f,
                epaisseur = 1.4f
            )
            courbe(
                depart = -0.1f to 0.95f,
                controle1 = 0.35f to 0.78f,
                controle2 = 0.70f to 1.05f,
                arrivee = 1.1f to 0.85f,
                opacite = 0.11f,
                epaisseur = 1.2f
            )
            courbe(
                depart = 0.18f to -0.05f,
                controle1 = 0.32f to 0.28f,
                controle2 = 0.12f to 0.52f,
                arrivee = 0.55f to 1.05f,
                opacite = 0.10f,
                epaisseur = 1.2f
            )
            courbe(
                depart = 0.88f to -0.05f,
                controle1 = 0.75f to 0.22f,
                controle2 = 1.02f to 0.40f,
                arrivee = 0.80f to 1.05f,
                opacite = 0.14f,
                epaisseur = 1.4f
            )
        }
    }
}