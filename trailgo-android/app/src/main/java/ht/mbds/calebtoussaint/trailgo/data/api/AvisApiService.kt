// data/api/AvisApiService.kt
package ht.mbds.calebtoussaint.trailgo.data.api

import ht.mbds.calebtoussaint.trailgo.data.model.AvisRequest
import ht.mbds.calebtoussaint.trailgo.data.model.AvisResponse
import ht.mbds.calebtoussaint.trailgo.data.model.PageResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface AvisApiService {

    @GET("api/parcours/{parcoursId}/avis")
    suspend fun lister(
        @Path("parcoursId") parcoursId: Long,
        @Query("page") page: Int = 0,
        @Query("size") taille: Int = 20
    ): PageResponse<AvisResponse>

    @POST("api/parcours/{parcoursId}/avis")
    suspend fun deposer(
        @Path("parcoursId") parcoursId: Long,
        @Body corps: AvisRequest
    ): AvisResponse
}