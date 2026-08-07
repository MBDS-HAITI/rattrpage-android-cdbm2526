// util/Haversine.kt
package ht.mbds.calebtoussaint.trailgo.util

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Calculs geodesiques locaux (distance et cap), utilises pour la
 * navigation terrain sans solliciter l'API a chaque position GPS.
 */
object Haversine {

    private const val RAYON_TERRE_METRES = 6_371_000.0

    /** Distance en metres entre deux points, en tenant compte de la courbure terrestre. */
    fun distanceMetres(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val deltaLat = Math.toRadians(lat2 - lat1)
        val deltaLng = Math.toRadians(lng2 - lng1)

        val a = sin(deltaLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(deltaLng / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return RAYON_TERRE_METRES * c
    }

    /**
     * Cap en degres (0 = nord, 90 = est...) a suivre depuis le point 1
     * vers le point 2.
     */
    fun capDegres(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaLambda = Math.toRadians(lng2 - lng1)

        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)

        val capBrut = Math.toDegrees(atan2(y, x))
        return (capBrut + 360) % 360
    }
}