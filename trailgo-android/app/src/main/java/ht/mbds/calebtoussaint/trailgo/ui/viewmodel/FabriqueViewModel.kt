// ui/viewmodel/FabriqueViewModel.kt
package ht.mbds.calebtoussaint.trailgo.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import ht.mbds.calebtoussaint.trailgo.data.api.ApiClient
import ht.mbds.calebtoussaint.trailgo.data.api.AuthApiService
import ht.mbds.calebtoussaint.trailgo.data.api.GestionnaireJeton
import ht.mbds.calebtoussaint.trailgo.data.api.ParcoursApiService
import ht.mbds.calebtoussaint.trailgo.data.repository.AuthRepository
import ht.mbds.calebtoussaint.trailgo.data.repository.ParcoursRepository

/**
 * Petit conteneur d'injection de dependances ecrit a la main.
 *
 * Pas de Hilt ni Dagger ici : pour un projet de cette taille, la
 * complexite additionnelle ne se justifie pas.
 */
object FabriqueViewModel {

    private var jeton: GestionnaireJeton? = null
    private var repositoryAuth: AuthRepository? = null
    private var repositoryParcours: ParcoursRepository? = null

    private fun obtenirGestionnaireJeton(context: Context): GestionnaireJeton {
        return jeton ?: GestionnaireJeton(context.applicationContext).also { jeton = it }
    }

    private fun obtenirRepositoryAuth(context: Context): AuthRepository {
        return repositoryAuth ?: run {
            val gestionnaireJeton = obtenirGestionnaireJeton(context)
            val retrofit = ApiClient.creerRetrofit(gestionnaireJeton)
            val authApi = retrofit.create(AuthApiService::class.java)
            AuthRepository(authApi, gestionnaireJeton).also { repositoryAuth = it }
        }
    }

    private fun obtenirRepositoryParcours(context: Context): ParcoursRepository {
        return repositoryParcours ?: run {
            val gestionnaireJeton = obtenirGestionnaireJeton(context)
            val retrofit = ApiClient.creerRetrofit(gestionnaireJeton)
            val parcoursApi = retrofit.create(ParcoursApiService::class.java)
            ParcoursRepository(parcoursApi).also { repositoryParcours = it }
        }
    }

    fun creerFabriqueConnexion(context: Context): ViewModelProvider.Factory {
        return object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return ConnexionViewModel(obtenirRepositoryAuth(context)) as T
            }
        }
    }

    fun creerFabriqueListeParcours(context: Context): ViewModelProvider.Factory {
        return object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return ListeParcoursViewModel(obtenirRepositoryParcours(context)) as T
            }
        }
    }

    fun creerFabriqueDetailParcours(context: Context): ViewModelProvider.Factory {
        return object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return DetailParcoursViewModel(obtenirRepositoryParcours(context)) as T
            }
        }
    }
    fun creerFabriqueCarteParcours(context: Context): ViewModelProvider.Factory {
        return object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return CarteParcoursViewModel(obtenirRepositoryParcours(context)) as T
            }
        }
    }
}