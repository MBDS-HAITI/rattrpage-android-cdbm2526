// ui/viewmodel/DetailParcoursViewModel.kt
package ht.mbds.calebtoussaint.trailgo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ht.mbds.calebtoussaint.trailgo.data.model.ParcoursResponse
import ht.mbds.calebtoussaint.trailgo.data.repository.FavorisRepository
import ht.mbds.calebtoussaint.trailgo.data.repository.ParcoursRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EtatDetailParcours(
    val parcours: ParcoursResponse? = null,
    val chargement: Boolean = true,
    val erreur: String? = null,
    val estFavori: Boolean = false,
    val chargementFavori: Boolean = false
)

class DetailParcoursViewModel(
    private val parcoursRepository: ParcoursRepository,
    private val favorisRepository: FavorisRepository
) : ViewModel() {

    private val _etat = MutableStateFlow(EtatDetailParcours())
    val etat: StateFlow<EtatDetailParcours> = _etat

    private var idCourant: Long? = null

    fun charger(id: Long) {
        if (idCourant == id && _etat.value.parcours != null) return
        idCourant = id

        viewModelScope.launch {
            _etat.update { it.copy(chargement = true, erreur = null) }

            parcoursRepository.consulter(id).fold(
                onSuccess = { detail ->
                    _etat.update { it.copy(chargement = false, parcours = detail) }
                    chargerStatutFavori(id)
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

    private fun chargerStatutFavori(id: Long) {
        // Appel separe, non bloquant : un echec ici (ex. hors ligne)
        // laisse simplement le coeur a son etat par defaut plutot que
        // de bloquer l'affichage du reste de la fiche.
        viewModelScope.launch {
            favorisRepository.verifierStatut(id).onSuccess { favori ->
                _etat.update { it.copy(estFavori = favori) }
            }
        }
    }

    fun togglerFavori() {
        val id = idCourant ?: return
        if (_etat.value.chargementFavori) return

        viewModelScope.launch {
            val etaitFavori = _etat.value.estFavori
            _etat.update { it.copy(chargementFavori = true) }

            val resultat = if (etaitFavori) {
                favorisRepository.retirer(id)
            } else {
                favorisRepository.ajouter(id)
            }

            resultat.fold(
                onSuccess = {
                    _etat.update { it.copy(chargementFavori = false, estFavori = !etaitFavori) }
                },
                onFailure = {
                    _etat.update { it.copy(chargementFavori = false) }
                }
            )
        }
    }

    fun reessayer() {
        val id = idCourant ?: return
        idCourant = null
        charger(id)
    }
}