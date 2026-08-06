// ui/viewmodel/ListeParcoursViewModel.kt
package ht.mbds.calebtoussaint.trailgo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ht.mbds.calebtoussaint.trailgo.data.model.ParcoursSummaryResponse
import ht.mbds.calebtoussaint.trailgo.data.repository.ParcoursRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EtatListeParcours(
    val parcours: List<ParcoursSummaryResponse> = emptyList(),
    val chargement: Boolean = true,
    val erreur: String? = null,
    val filtreTheme: String? = null,
    val filtreDifficulte: String? = null,
    val recherche: String = ""
)

class ListeParcoursViewModel(
    private val parcoursRepository: ParcoursRepository
) : ViewModel() {

    private val _etat = MutableStateFlow(EtatListeParcours())
    val etat: StateFlow<EtatListeParcours> = _etat

    init {
        charger()
    }

    fun charger() {
        viewModelScope.launch {
            _etat.update { it.copy(chargement = true, erreur = null) }

            val etatActuel = _etat.value
            val resultat = parcoursRepository.lister(
                theme = etatActuel.filtreTheme,
                difficulte = etatActuel.filtreDifficulte,
                // Un touriste ne doit voir que les parcours publies.
                statut = "PUBLIE",
                recherche = etatActuel.recherche.ifBlank { null }
            )

            resultat.fold(
                onSuccess = { page ->
                    _etat.update { it.copy(chargement = false, parcours = page.contenu) }
                },
                onFailure = {
                    _etat.update {
                        it.copy(
                            chargement = false,
                            erreur = "Impossible de charger les parcours. Verifiez votre connexion."
                        )
                    }
                }
            )
        }
    }

    fun changerFiltreTheme(theme: String?) {
        _etat.update { it.copy(filtreTheme = theme) }
        charger()
    }

    fun changerFiltreDifficulte(difficulte: String?) {
        _etat.update { it.copy(filtreDifficulte = difficulte) }
        charger()
    }

    fun changerRecherche(texte: String) {
        _etat.update { it.copy(recherche = texte) }
        charger()
    }
}
