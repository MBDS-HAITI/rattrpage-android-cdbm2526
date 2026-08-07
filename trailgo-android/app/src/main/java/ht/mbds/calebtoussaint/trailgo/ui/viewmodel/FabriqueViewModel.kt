// ui/viewmodel/FabriqueViewModel.kt
package ht.mbds.calebtoussaint.trailgo.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.room.Room
import ht.mbds.calebtoussaint.trailgo.data.api.ApiClient
import ht.mbds.calebtoussaint.trailgo.data.api.AuthApiService
import ht.mbds.calebtoussaint.trailgo.data.api.AvisApiService
import ht.mbds.calebtoussaint.trailgo.data.api.FavorisApiService
import ht.mbds.calebtoussaint.trailgo.data.api.GestionnaireJeton
import ht.mbds.calebtoussaint.trailgo.data.api.ParcoursApiService
import ht.mbds.calebtoussaint.trailgo.data.local.TrailGoDatabase
import ht.mbds.calebtoussaint.trailgo.data.repository.AuthRepository
import ht.mbds.calebtoussaint.trailgo.data.repository.AvisRepository
import ht.mbds.calebtoussaint.trailgo.data.repository.FavorisRepository
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
    private var repositoryFavoris: FavorisRepository? = null
    private var repositoryAvis: AvisRepository? = null
    private var baseDeDonnees: TrailGoDatabase? = null

    private fun obtenirGestionnaireJeton(context: Context): GestionnaireJeton {
        return jeton ?: GestionnaireJeton(context.applicationContext).also { jeton = it }
    }

    private fun obtenirBaseDeDonnees(context: Context): TrailGoDatabase {
        return baseDeDonnees ?: Room.databaseBuilder(
            context.applicationContext,
            TrailGoDatabase::class.java,
            "trailgo.db"
        ).build().also { baseDeDonnees = it }
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
            val cacheDao = obtenirBaseDeDonnees(context).parcoursCacheDao()
            ParcoursRepository(parcoursApi, cacheDao).also { repositoryParcours = it }
        }
    }

    private fun obtenirRepositoryFavoris(context: Context): FavorisRepository {
        return repositoryFavoris ?: run {
            val gestionnaireJeton = obtenirGestionnaireJeton(context)
            val retrofit = ApiClient.creerRetrofit(gestionnaireJeton)
            val favorisApi = retrofit.create(FavorisApiService::class.java)
            FavorisRepository(favorisApi).also { repositoryFavoris = it }
        }
    }

    private fun obtenirRepositoryAvis(context: Context): AvisRepository {
        return repositoryAvis ?: run {
            val gestionnaireJeton = obtenirGestionnaireJeton(context)
            val retrofit = ApiClient.creerRetrofit(gestionnaireJeton)
            val avisApi = retrofit.create(AvisApiService::class.java)
            AvisRepository(avisApi).also { repositoryAvis = it }
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
                return DetailParcoursViewModel(
                    obtenirRepositoryParcours(context),
                    obtenirRepositoryFavoris(context)
                ) as T
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

    fun creerFabriqueFavoris(context: Context): ViewModelProvider.Factory {
        return object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return FavorisViewModel(obtenirRepositoryFavoris(context)) as T
            }
        }
    }

    fun creerFabriqueAvis(context: Context): ViewModelProvider.Factory {
        return object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return AvisViewModel(obtenirRepositoryAvis(context)) as T
            }
        }
    }
}