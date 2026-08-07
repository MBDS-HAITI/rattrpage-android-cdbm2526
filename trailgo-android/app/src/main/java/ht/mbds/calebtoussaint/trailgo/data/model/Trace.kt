// data/model/Trace.kt
package ht.mbds.calebtoussaint.trailgo.data.model

/**
 * Tracé d'un parcours, renvoyé par GET /api/parcours/{id}/trace.
 *
 * Les coordonnees sont imbriquees dans le champ "geometrie" (format
 * GeoJSON LineString), pas a la racine de la reponse.
 *
 * Ordre GeoJSON (RFC 7946) : chaque paire est [longitude, latitude],
 * l'inverse de l'usage GPS habituel.
 */
data class TraceResponse(
    val parcoursId: Long,
    val geometrie: GeometrieResponse,
    val distanceKm: Double?,
    val nbPoints: Int?,
    val bbox: List<Double>?
)

data class GeometrieResponse(
    val type: String,
    val coordinates: List<List<Double>>
)