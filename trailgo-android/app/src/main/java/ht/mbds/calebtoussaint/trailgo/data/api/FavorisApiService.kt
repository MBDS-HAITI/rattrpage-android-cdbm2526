// data/api/FavorisApiService.kt
package ht.mbds.calebtoussaint.trailgo.data.api

import ht.mbds.calebtoussaint.trailgo.data.model.FavoriResponse
import ht.mbds.calebtoussaint.trailgo.data.model.PageResponse
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface FavorisApiService {

    @GET("api/favoris")
    suspend fun lister(
        @Query("page") page: Int = 0,
        @Query("size") taille: Int = 20
    ): PageResponse<FavoriResponse>

    @POST("api/favoris/{parcoursId}")
    suspend fun ajouter(@Path("parcoursId") parcoursId: Long): FavoriResponse

    // 204 No Content : pas de corps a desserialiser, Response<Unit>
    // permet de verifier isSuccessful() sans que Gson n'echoue sur un
    // corps vide.
    @DELETE("api/favoris/{parcoursId}")
    suspend fun retirer(@Path("parcoursId") parcoursId: Long): Response<Unit>

    @GET("api/favoris/{parcoursId}/statut")
    suspend fun verifierStatut(@Path("parcoursId") parcoursId: Long): Map<String, Boolean>
}