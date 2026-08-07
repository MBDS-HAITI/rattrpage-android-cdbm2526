// data/repository/FavorisRepository.kt
package ht.mbds.calebtoussaint.trailgo.data.repository

import ht.mbds.calebtoussaint.trailgo.data.api.FavorisApiService
import ht.mbds.calebtoussaint.trailgo.data.model.FavoriResponse
import ht.mbds.calebtoussaint.trailgo.data.model.PageResponse

class FavorisRepository(
    private val favorisApi: FavorisApiService
) {
    suspend fun lister(page: Int = 0): Result<PageResponse<FavoriResponse>> {
        return try {
            Result.success(favorisApi.lister(page = page))
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    suspend fun ajouter(parcoursId: Long): Result<FavoriResponse> {
        return try {
            Result.success(favorisApi.ajouter(parcoursId))
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    suspend fun retirer(parcoursId: Long): Result<Unit> {
        return try {
            val reponse = favorisApi.retirer(parcoursId)
            if (reponse.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Erreur ${reponse.code()}"))
            }
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    /**
     * La cle exacte renvoyee par l'API n'est pas garantie (schema
     * Swagger generique de type Map<String, Boolean>). On considere le
     * parcours en favori si au moins une valeur de la map est vraie,
     * independamment de son nom de cle.
     */
    suspend fun verifierStatut(parcoursId: Long): Result<Boolean> {
        return try {
            val carte = favorisApi.verifierStatut(parcoursId)
            Result.success(carte.values.any { it })
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }
}