// ui/viewmodel/ConnexionViewModel.kt
package ht.mbds.calebtoussaint.trailgo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ht.mbds.calebtoussaint.trailgo.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * L'etat complet de l'ecran de connexion, a un instant donne.
 *
 * "data class" : chaque modification cree une COPIE avec un seul champ
 * change (via .copy()), plutot que de muter l'etat en place. C'est ce
 * qui permet a Compose de detecter precisement ce qui a change et de
 * ne redessiner que ce qui est necessaire.
 */
data class EtatConnexion(
    val email: String = "",
    val motDePasse: String = "",
    val chargement: Boolean = false,
    val erreur: String? = null,
    val connexionReussie: Boolean = false
)

/**
 * ViewModel de l'ecran de connexion.
 *
 * Survit aux rotations d'ecran (contrairement a un etat garde
 * directement dans le composant Compose), et ne connait rien de
 * l'affichage : il expose seulement un etat et des actions.
 *
 * Comparable au hook useAuth() + useState() cote React, mais avec la
 * logique et l'etat entierement separes du rendu visuel.
 */
class ConnexionViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    // MutableStateFlow : la version modifiable, privee. StateFlow
    // (ci-dessous) : la version en lecture seule, exposee a l'ecran.
    // Cette distinction empeche l'ecran de modifier l'etat directement,
    // il ne peut que le lire et declencher des actions.
    private val _etat = MutableStateFlow(EtatConnexion())
    val etat: StateFlow<EtatConnexion> = _etat

    fun modifierEmail(valeur: String) {
        _etat.update { it.copy(email = valeur, erreur = null) }
    }

    fun modifierMotDePasse(valeur: String) {
        _etat.update { it.copy(motDePasse = valeur, erreur = null) }
    }

    /**
     * viewModelScope : les coroutines lancees ici sont automatiquement
     * annulees si le ViewModel est detruit (ecran quitte), evitant les
     * fuites memoire et les mises a jour d'un ecran qui n'existe plus.
     */
    fun connecter() {
        val etatActuel = _etat.value

        if (etatActuel.email.isBlank() || etatActuel.motDePasse.isBlank()) {
            _etat.update { it.copy(erreur = "Email et mot de passe obligatoires") }
            return
        }

        viewModelScope.launch {
            _etat.update { it.copy(chargement = true, erreur = null) }

            val resultat = authRepository.connecter(etatActuel.email, etatActuel.motDePasse)

            resultat.fold(
                onSuccess = {
                    _etat.update { it.copy(chargement = false, connexionReussie = true) }
                },
                onFailure = { exception ->
                    // Message generique : le detail exact de l'erreur HTTP
                    // sera affine une fois le format d'erreur de l'API
                    // (ProblemDetail) analyse cote client, a l'etape suivante.
                    _etat.update {
                        it.copy(
                            chargement = false,
                            erreur = "Email ou mot de passe incorrect"
                        )
                    }
                }
            )
        }
    }
}
