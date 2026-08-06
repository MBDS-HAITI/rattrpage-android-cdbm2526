// data/api/ParcoursApiService.kt
package ht.mbds.calebtoussaint.trailgo.data.api

import ht.mbds.calebtoussaint.trailgo.data.model.PageResponse
import ht.mbds.calebtoussaint.trailgo.data.model.ParcoursResponse
import ht.mbds.calebtoussaint.trailgo.data.model.ParcoursSummaryResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ParcoursApiService {

    /**
     * Chaque parametre est optionnel (nullable, valeur par defaut null).
     * Retrofit omet automatiquement de l'URL les parametres nuls,
     * exactement comme le faisait le client Axios cote React.
     */
    @GET("api/parcours")
    suspend fun lister(
        @Query("page") page: Int = 0,
        @Query("size") taille: Int = 20,
        @Query("theme") theme: String? = null,
        @Query("difficulte") difficulte: String? = null,
        @Query("statut") statut: String? = null,
        @Query("recherche") recherche: String? = null
    ): PageResponse<ParcoursSummaryResponse>

    @GET("api/parcours/{id}")
    suspend fun consulter(@Path("id") id: Long): ParcoursResponse
}
