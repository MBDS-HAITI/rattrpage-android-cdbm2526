// ui/viewmodel/FavorisViewModel.kt
package ht.mbds.calebtoussaint.trailgo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ht.mbds.calebtoussaint.trailgo.data.model.FavoriResponse
import ht.mbds.calebtoussaint.trailgo.data.repository.FavorisRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EtatFavoris(
    val favoris: List<FavoriResponse> = emptyList(),
    val chargement: Boolean = true,
    val erreur: String? = null
)

class FavorisViewModel(
    private val favorisRepository: FavorisRepository
) : ViewModel() {

    private val _etat = MutableStateFlow(EtatFavoris())
    val etat: StateFlow<EtatFavoris> = _etat

    init {
        charger()
    }

    fun charger() {
        viewModelScope.launch {
            _etat.update { it.copy(chargement = true, erreur = null) }

            favorisRepository.lister().fold(
                onSuccess = { page ->
                    _etat.update { it.copy(chargement = false, favoris = page.contenu) }
                },
                onFailure = {
                    _etat.update {
                        it.copy(
                            chargement = false,
                            erreur = "Impossible de charger vos favoris. Verifiez votre connexion."
                        )
                    }
                }
            )
        }
    }

    fun retirer(parcoursId: Long) {
        viewModelScope.launch {
            favorisRepository.retirer(parcoursId).onSuccess {
                _etat.update { etat ->
                    etat.copy(favoris = etat.favoris.filterNot { it.parcours.id == parcoursId })
                }
            }
        }
    }
}