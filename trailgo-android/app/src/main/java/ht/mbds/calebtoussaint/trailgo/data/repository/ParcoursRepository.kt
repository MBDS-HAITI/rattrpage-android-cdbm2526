// data/repository/ParcoursRepository.kt
package ht.mbds.calebtoussaint.trailgo.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ht.mbds.calebtoussaint.trailgo.data.api.ParcoursApiService
import ht.mbds.calebtoussaint.trailgo.data.local.DetailParcoursCacheEntity
import ht.mbds.calebtoussaint.trailgo.data.local.ListeParcoursCacheEntity
import ht.mbds.calebtoussaint.trailgo.data.local.ParcoursCacheDao
import ht.mbds.calebtoussaint.trailgo.data.local.TraceParcoursCacheEntity
import ht.mbds.calebtoussaint.trailgo.data.model.PageResponse
import ht.mbds.calebtoussaint.trailgo.data.model.ParcoursResponse
import ht.mbds.calebtoussaint.trailgo.data.model.ParcoursSummaryResponse
import ht.mbds.calebtoussaint.trailgo.data.model.TraceResponse
import java.io.IOException

/**
 * Cache Room active uniquement en cas d'erreur reseau (IOException) :
 * pas de connexion, delai depasse, hote injoignable. Les autres
 * erreurs (401, 404, 500...) remontent normalement sans passer par le
 * cache, pour ne jamais masquer un vrai probleme cote API derriere un
 * faux succes hors ligne.
 */
class ParcoursRepository(
    private val parcoursApi: ParcoursApiService,
    private val cacheDao: ParcoursCacheDao
) {
    private val gson = Gson()

    suspend fun lister(
        page: Int = 0,
        theme: String? = null,
        difficulte: String? = null,
        statut: String? = null,
        recherche: String? = null
    ): Result<PageResponse<ParcoursSummaryResponse>> {
        return try {
            val resultat = parcoursApi.lister(
                page = page,
                theme = theme,
                difficulte = difficulte,
                statut = statut,
                recherche = recherche
            )

            // Seule la vue par defaut (page 0, aucun filtre) est mise en
            // cache : c'est elle que l'utilisateur retrouvera hors ligne.
            if (page == 0 && theme == null && difficulte == null && recherche == null) {
                cacheDao.enregistrerListe(
                    ListeParcoursCacheEntity(
                        json = gson.toJson(resultat),
                        dateMiseAJour = System.currentTimeMillis()
                    )
                )
            }

            Result.success(resultat)
        } catch (exception: IOException) {
            val entite = cacheDao.recupererListe()
            if (entite != null) {
                val type = object : TypeToken<PageResponse<ParcoursSummaryResponse>>() {}.type
                Result.success(gson.fromJson(entite.json, type))
            } else {
                Result.failure(exception)
            }
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    suspend fun consulter(id: Long): Result<ParcoursResponse> {
        return try {
            val resultat = parcoursApi.consulter(id)
            cacheDao.enregistrerDetail(
                DetailParcoursCacheEntity(
                    parcoursId = id,
                    json = gson.toJson(resultat),
                    dateMiseAJour = System.currentTimeMillis()
                )
            )
            Result.success(resultat)
        } catch (exception: IOException) {
            val entite = cacheDao.recupererDetail(id)
            if (entite != null) {
                Result.success(gson.fromJson(entite.json, ParcoursResponse::class.java))
            } else {
                Result.failure(exception)
            }
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    suspend fun consulterTrace(id: Long): Result<TraceResponse> {
        return try {
            val resultat = parcoursApi.consulterTrace(id)
            cacheDao.enregistrerTrace(
                TraceParcoursCacheEntity(
                    parcoursId = id,
                    json = gson.toJson(resultat),
                    dateMiseAJour = System.currentTimeMillis()
                )
            )
            Result.success(resultat)
        } catch (exception: IOException) {
            val entite = cacheDao.recupererTrace(id)
            if (entite != null) {
                Result.success(gson.fromJson(entite.json, TraceResponse::class.java))
            } else {
                Result.failure(exception)
            }
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }
}