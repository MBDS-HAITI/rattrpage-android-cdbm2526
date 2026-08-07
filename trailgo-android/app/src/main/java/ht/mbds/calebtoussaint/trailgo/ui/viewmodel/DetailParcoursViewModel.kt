// ui/viewmodel/DetailParcoursViewModel.kt
package ht.mbds.calebtoussaint.trailgo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ht.mbds.calebtoussaint.trailgo.data.model.ParcoursResponse
import ht.mbds.calebtoussaint.trailgo.data.repository.ParcoursRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EtatDetailParcours(
    val parcours: ParcoursResponse? = null,
    val chargement: Boolean = true,
    val erreur: String? = null
)

class DetailParcoursViewModel(
    private val parcoursRepository: ParcoursRepository
) : ViewModel() {

    private val _etat = MutableStateFlow(EtatDetailParcours())
    val etat: StateFlow<EtatDetailParcours> = _etat

    // Memorise pour permettre un "Reessayer" sans que l'ecran
    // ait a repasser l'identifiant.
    private var idCourant: Long? = null

    fun charger(id: Long) {
        // Evite de relancer l'appel a chaque recomposition de l'ecran.
        if (idCourant == id && _etat.value.parcours != null) return
        idCourant = id

        viewModelScope.launch {
            _etat.update { it.copy(chargement = true, erreur = null) }

            parcoursRepository.consulter(id).fold(
                onSuccess = { detail ->
                    _etat.update { it.copy(chargement = false, parcours = detail) }
                },
                onFailure = {
                    _etat.update {
                        it.copy(
                            chargement = false,
                            erreur = "Impossible de charger ce parcours. Verifiez votre connexion."
                        )
                    }
                }
            )
        }
    }

    fun reessayer() {
        val id = idCourant ?: return
        // On force le rechargement en ignorant le garde-fou de charger().
        idCourant = null
        charger(id)
    }
}