// ui/viewmodel/FabriqueViewModel.kt
package ht.mbds.calebtoussaint.trailgo.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import ht.mbds.calebtoussaint.trailgo.data.api.ApiClient
import ht.mbds.calebtoussaint.trailgo.data.api.AuthApiService
import ht.mbds.calebtoussaint.trailgo.data.api.GestionnaireJeton
import ht.mbds.calebtoussaint.trailgo.data.repository.AuthRepository

/**
 * Petit conteneur d'injection de dependances ecrit a la main.
 *
 * Pas de Hilt ni Dagger ici : pour un projet de cette taille, la
 * complexite additionnelle ne se justifie pas. Cette fabrique cree
 * la chaine complete GestionnaireJeton -> Retrofit -> ApiService ->
 * Repository, et fournit un repository partage a tous les ViewModel.
 *
 * "object" en Kotlin = singleton automatique, une seule instance
 * existe pour toute l'application.
 */
object FabriqueViewModel {

    private var repositoryAuth: AuthRepository? = null

    /** Construit (ou reutilise) le AuthRepository, partage par toute l'app. */
    private fun obtenirRepositoryAuth(context: Context): AuthRepository {
        return repositoryAuth ?: run {
            val gestionnaireJeton = GestionnaireJeton(context.applicationContext)
            val retrofit = ApiClient.creerRetrofit(gestionnaireJeton)
            val authApi = retrofit.create(AuthApiService::class.java)
            AuthRepository(authApi, gestionnaireJeton).also { repositoryAuth = it }
        }
    }

    /**
     * Fabrique generique pour ConnexionViewModel.
     * ViewModelProvider.Factory est l'interface standard qu'Android
     * utilise pour construire un ViewModel avec des dependances
     * (un ViewModel ne peut pas avoir un constructeur vide s'il a
     * besoin d'un repository).
     */
    fun creerFabriqueConnexion(context: Context): ViewModelProvider.Factory {
        return object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                return ConnexionViewModel(obtenirRepositoryAuth(context)) as T
            }
        }
    }
}
