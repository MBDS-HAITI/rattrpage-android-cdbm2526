// ui/viewmodel/CarteParcoursViewModel.kt
package ht.mbds.calebtoussaint.trailgo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ht.mbds.calebtoussaint.trailgo.data.model.EtapeResponse
import ht.mbds.calebtoussaint.trailgo.data.repository.ParcoursRepository
import ht.mbds.calebtoussaint.trailgo.util.Haversine
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

/** Distance sous laquelle une etape est consideree comme atteinte. */
private const val SEUIL_ARRIVEE_METRES = 30.0

data class EtatCarteParcours(
    val titre: String = "",
    val etapes: List<EtapeResponse> = emptyList(),
    val trace: List<GeoPoint> = emptyList(),
    val chargement: Boolean = true,
    val erreur: String? = null,

    // ---- Navigation terrain ----
    val modeNavigation: Boolean = false,
    val positionUtilisateur: GeoPoint? = null,
    val etapesAtteintes: Set<Long> = emptySet(),
    val distanceProchaineEtapeM: Double? = null,
    val capProchaineEtapeDeg: Double? = null
) {
    /** Prochaine etape non encore atteinte, ou null si tout est fait. */
    val prochaineEtape: EtapeResponse?
        get() = etapes.firstOrNull { it.id !in etapesAtteintes }
}

class CarteParcoursViewModel(
    private val parcoursRepository: ParcoursRepository
) : ViewModel() {

    private val _etat = MutableStateFlow(EtatCarteParcours())
    val etat: StateFlow<EtatCarteParcours> = _etat

    private var idCourant: Long? = null

    fun charger(id: Long) {
        if (idCourant == id && _etat.value.trace.isNotEmpty()) return
        idCourant = id

        viewModelScope.launch {
            _etat.update { it.copy(chargement = true, erreur = null) }

            coroutineScope {
                val resultatDetail = async { parcoursRepository.consulter(id) }
                val resultatTrace = async { parcoursRepository.consulterTrace(id) }

                val detail = resultatDetail.await()
                val trace = resultatTrace.await()

                if (detail.isFailure) {
                    _etat.update {
                        it.copy(
                            chargement = false,
                            erreur = "Impossible de charger la carte. Verifiez votre connexion."
                        )
                    }
                    return@coroutineScope
                }

                val parcours = detail.getOrThrow()
                val points = trace.getOrNull()?.geometrie?.coordinates?.map { paire ->
                    GeoPoint(paire[1], paire[0])
                } ?: emptyList()

                _etat.update {
                    it.copy(
                        chargement = false,
                        titre = parcours.titre,
                        etapes = parcours.etapes.sortedBy { etape -> etape.ordre },
                        trace = points
                    )
                }
            }
        }
    }

    fun reessayer() {
        val id = idCourant ?: return
        idCourant = null
        charger(id)
    }

    // ---- Navigation terrain ----

    fun demarrerNavigation() {
        _etat.update { it.copy(modeNavigation = true) }
    }

    fun arreterNavigation() {
        _etat.update {
            it.copy(
                modeNavigation = false,
                positionUtilisateur = null,
                distanceProchaineEtapeM = null,
                capProchaineEtapeDeg = null
            )
        }
    }

    /**
     * Appelee a chaque nouvelle position GPS recue. Met a jour la
     * distance et le cap vers la prochaine etape non atteinte, et la
     * marque comme atteinte si l'utilisateur entre dans le rayon de
     * detection (Haversine local).
     */
    fun mettreAJourPosition(latitude: Double, longitude: Double) {
        val etatActuel = _etat.value
        val prochaine = etatActuel.prochaineEtape

        if (prochaine == null) {
            _etat.update { it.copy(positionUtilisateur = GeoPoint(latitude, longitude)) }
            return
        }

        val distance = Haversine.distanceMetres(
            latitude, longitude, prochaine.latitude, prochaine.longitude
        )
        val cap = Haversine.capDegres(
            latitude, longitude, prochaine.latitude, prochaine.longitude
        )

        val nouvellesAtteintes = if (distance <= SEUIL_ARRIVEE_METRES) {
            etatActuel.etapesAtteintes + prochaine.id
        } else {
            etatActuel.etapesAtteintes
        }

        _etat.update {
            it.copy(
                positionUtilisateur = GeoPoint(latitude, longitude),
                distanceProchaineEtapeM = distance,
                capProchaineEtapeDeg = cap,
                etapesAtteintes = nouvellesAtteintes
            )
        }
    }
}