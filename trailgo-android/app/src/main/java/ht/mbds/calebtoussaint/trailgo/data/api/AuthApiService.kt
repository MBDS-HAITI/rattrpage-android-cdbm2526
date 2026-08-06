// data/api/AuthApiService.kt
package ht.mbds.calebtoussaint.trailgo.data.api

import ht.mbds.calebtoussaint.trailgo.data.model.AuthResponse
import ht.mbds.calebtoussaint.trailgo.data.model.ConnexionRequest
import ht.mbds.calebtoussaint.trailgo.data.model.InscriptionRequest
import ht.mbds.calebtoussaint.trailgo.data.model.UtilisateurResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Interface Retrofit : chaque fonction correspond a un endpoint de
 * l'API. Retrofit genere l'implementation automatiquement a partir
 * des annotations, comme les Repository Spring Data generent leurs
 * requetes a partir du nom de la methode.
 *
 * "suspend" marque une fonction executable dans une coroutine : elle
 * peut suspendre son execution (attendre le reseau) sans bloquer le
 * thread principal, donc sans figer l'interface pendant l'appel.
 */
interface AuthApiService {

    @POST("api/auth/connexion")
    suspend fun connecter(@Body requete: ConnexionRequest): AuthResponse

    @POST("api/auth/inscription")
    suspend fun inscrire(@Body requete: InscriptionRequest): AuthResponse

    @GET("api/auth/moi")
    suspend fun obtenirProfil(): UtilisateurResponse
}
