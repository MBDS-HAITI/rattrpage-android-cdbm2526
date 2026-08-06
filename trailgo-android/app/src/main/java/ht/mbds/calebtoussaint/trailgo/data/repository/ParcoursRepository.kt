// data/repository/ParcoursRepository.kt
package ht.mbds.calebtoussaint.trailgo.data.repository

import ht.mbds.calebtoussaint.trailgo.data.api.ParcoursApiService
import ht.mbds.calebtoussaint.trailgo.data.model.PageResponse
import ht.mbds.calebtoussaint.trailgo.data.model.ParcoursResponse
import ht.mbds.calebtoussaint.trailgo.data.model.ParcoursSummaryResponse

class ParcoursRepository(
    private val parcoursApi: ParcoursApiService
) {
    suspend fun lister(
        page: Int = 0,
        theme: String? = null,
        difficulte: String? = null,
        statut: String? = null,
        recherche: String? = null
    ): Result<PageResponse<ParcoursSummaryResponse>> {
        return try {
            Result.success(
                parcoursApi.lister(
                    page = page,
                    theme = theme,
                    difficulte = difficulte,
                    statut = statut,
                    recherche = recherche
                )
            )
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    suspend fun consulter(id: Long): Result<ParcoursResponse> {
        return try {
            Result.success(parcoursApi.consulter(id))
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }
}
