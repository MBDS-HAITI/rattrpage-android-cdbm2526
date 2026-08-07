// ui/viewmodel/CarteParcoursViewModel.kt
package ht.mbds.calebtoussaint.trailgo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ht.mbds.calebtoussaint.trailgo.data.model.EtapeResponse
import ht.mbds.calebtoussaint.trailgo.data.repository.ParcoursRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

data class EtatCarteParcours(
    val titre: String = "",
    val etapes: List<EtapeResponse> = emptyList(),
    val trace: List<GeoPoint> = emptyList(),
    val chargement: Boolean = true,
    val erreur: String? = null
)

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

            // Le detail (pour les etapes) et le trace sont deux appels
            // independants : on les lance en parallele plutot que l'un
            // apres l'autre, pour reduire le temps de chargement.
            coroutineScope {
                val resultatDetail = async { parcoursRepository.consulter(id) }
                val resultatTrace = async { parcoursRepository.consulterTrace(id) }

                val detail = resultatDetail.await()
                val trace = resultatTrace.await()

                // Le detail est indispensable (etapes, titre). Le trace est
                // optionnel : un parcours peut ne pas encore avoir de trace
                // importee, auquel cas la carte affiche les marqueurs seuls.
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
                    // GeoJSON : [longitude, latitude], inverse de GeoPoint(lat, lng).
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
}