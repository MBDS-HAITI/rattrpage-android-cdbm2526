// ui/viewmodel/AvisViewModel.kt
package ht.mbds.calebtoussaint.trailgo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ht.mbds.calebtoussaint.trailgo.data.model.AvisResponse
import ht.mbds.calebtoussaint.trailgo.data.repository.AvisRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class EtatAvis(
    val avis: List<AvisResponse> = emptyList(),
    val chargement: Boolean = true,

    // ---- Formulaire de depot ----
    val formulaireOuvert: Boolean = false,
    val noteSaisie: Int = 5,
    val commentaireSaisi: String = "",
    val envoiEnCours: Boolean = false,
    val erreurEnvoi: String? = null
)

class AvisViewModel(
    private val avisRepository: AvisRepository
) : ViewModel() {

    private val _etat = MutableStateFlow(EtatAvis())
    val etat: StateFlow<EtatAvis> = _etat

    private var idParcoursCourant: Long? = null

    fun charger(parcoursId: Long) {
        idParcoursCourant = parcoursId
        viewModelScope.launch {
            _etat.update { it.copy(chargement = true) }

            avisRepository.lister(parcoursId).fold(
                onSuccess = { page ->
                    _etat.update { it.copy(chargement = false, avis = page.contenu) }
                },
                onFailure = {
                    // Echec silencieux : la liste des avis reste vide,
                    // sans bloquer le reste de la fiche detail.
                    _etat.update { it.copy(chargement = false) }
                }
            )
        }
    }

    fun ouvrirFormulaire() {
        _etat.update { it.copy(formulaireOuvert = true, erreurEnvoi = null) }
    }

    fun fermerFormulaire() {
        _etat.update {
            it.copy(
                formulaireOuvert = false,
                noteSaisie = 5,
                commentaireSaisi = "",
                erreurEnvoi = null
            )
        }
    }

    fun modifierNoteSaisie(note: Int) {
        _etat.update { it.copy(noteSaisie = note) }
    }

    fun modifierCommentaireSaisi(texte: String) {
        _etat.update { it.copy(commentaireSaisi = texte) }
    }

    fun soumettre() {
        val parcoursId = idParcoursCourant ?: return
        if (_etat.value.envoiEnCours) return

        viewModelScope.launch {
            _etat.update { it.copy(envoiEnCours = true, erreurEnvoi = null) }

            avisRepository.deposer(
                parcoursId,
                _etat.value.noteSaisie,
                _etat.value.commentaireSaisi
            ).fold(
                onSuccess = { nouvelAvis ->
                    _etat.update {
                        it.copy(
                            envoiEnCours = false,
                            formulaireOuvert = false,
                            noteSaisie = 5,
                            commentaireSaisi = "",
                            avis = listOf(nouvelAvis) + it.avis
                        )
                    }
                },
                onFailure = { exception ->
                    _etat.update {
                        it.copy(envoiEnCours = false, erreurEnvoi = messageErreur(exception))
                    }
                }
            )
        }
    }

    private fun messageErreur(exception: Throwable): String {
        return if (exception is HttpException) {
            when (exception.code()) {
                409 -> "Vous avez deja depose un avis sur ce parcours."
                400 -> "La note doit etre comprise entre 1 et 5."
                403 -> "Vous devez etre connecte pour deposer un avis."
                else -> "Impossible d'envoyer votre avis. Verifiez votre connexion."
            }
        } else {
            "Impossible d'envoyer votre avis. Verifiez votre connexion."
        }
    }
}