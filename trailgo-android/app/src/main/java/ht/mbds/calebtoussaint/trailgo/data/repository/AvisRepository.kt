// data/repository/AvisRepository.kt
package ht.mbds.calebtoussaint.trailgo.data.repository

import ht.mbds.calebtoussaint.trailgo.data.api.AvisApiService
import ht.mbds.calebtoussaint.trailgo.data.model.AvisRequest
import ht.mbds.calebtoussaint.trailgo.data.model.AvisResponse
import ht.mbds.calebtoussaint.trailgo.data.model.PageResponse

class AvisRepository(
    private val avisApi: AvisApiService
) {
    suspend fun lister(parcoursId: Long, page: Int = 0): Result<PageResponse<AvisResponse>> {
        return try {
            Result.success(avisApi.lister(parcoursId, page))
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    suspend fun deposer(parcoursId: Long, note: Int, commentaire: String?): Result<AvisResponse> {
        return try {
            val corps = AvisRequest(note = note, commentaire = commentaire?.ifBlank { null })
            Result.success(avisApi.deposer(parcoursId, corps))
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }
}