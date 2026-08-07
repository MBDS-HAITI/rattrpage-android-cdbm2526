// data/location/GestionnaireLocalisation.kt
package ht.mbds.calebtoussaint.trailgo.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Enveloppe FusedLocationProviderClient dans un Flow, pour une
 * consommation naturelle depuis un composable (collect), plutot que
 * des callbacks Java classiques.
 *
 * L'appelant doit avoir verifie la permission ACCESS_FINE_LOCATION
 * avant de collecter ce flux : c'est la raison du @SuppressLint, le
 * compilateur ne peut pas savoir que la verification a ete faite en
 * amont, cote composable.
 */
class GestionnaireLocalisation(contexte: Context) {

    private val clientFusionne =
        LocationServices.getFusedLocationProviderClient(contexte.applicationContext)

    @SuppressLint("MissingPermission")
    fun positionsEnFlux(): Flow<Location> = callbackFlow {
        val requete = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3_000L)
            .setMinUpdateIntervalMillis(2_000L)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(resultat: LocationResult) {
                resultat.lastLocation?.let { position -> trySend(position) }
            }
        }

        clientFusionne.requestLocationUpdates(requete, callback, null)

        // Arrete proprement les mises a jour GPS quand personne ne
        // collecte plus le flux (ecran ferme, navigation quittee).
        awaitClose { clientFusionne.removeLocationUpdates(callback) }
    }
}