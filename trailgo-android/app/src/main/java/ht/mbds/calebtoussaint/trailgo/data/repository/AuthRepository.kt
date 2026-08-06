// data/repository/AuthRepository.kt
package ht.mbds.calebtoussaint.trailgo.data.repository

import ht.mbds.calebtoussaint.trailgo.data.api.AuthApiService
import ht.mbds.calebtoussaint.trailgo.data.api.GestionnaireJeton
import ht.mbds.calebtoussaint.trailgo.data.model.ConnexionRequest
import ht.mbds.calebtoussaint.trailgo.data.model.InscriptionRequest

/**
 * Repository : couche intermediaire entre le ViewModel et l'API.
 *
 * Meme role que le service Spring Boot cote back : le ViewModel ne
 * parle jamais directement a Retrofit, il passe toujours par ici.
 * Avantage concret : si demain on ajoute un cache Room pour le mode
 * hors ligne, seul ce fichier change, aucun ecran n'est impacte.
 */
class AuthRepository(
    private val authApi: AuthApiService,
    private val gestionnaireJeton: GestionnaireJeton
) {

    /**
     * Connecte l'utilisateur et enregistre la session localement en cas
     * de succes. Le "Result<Unit>" permet au ViewModel de distinguer
     * proprement succes et echec sans exception non geree.
     */
    suspend fun connecter(email: String, motDePasse: String): Result<Unit> {
        return try {
            val reponse = authApi.connecter(ConnexionRequest(email, motDePasse))
            gestionnaireJeton.enregistrerSession(
                jeton = reponse.jeton,
                utilisateurId = reponse.id,
                email = reponse.email,
                nom = reponse.nom,
                role = reponse.role
            )
            Result.success(Unit)
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    suspend fun inscrire(email: String, motDePasse: String, nom: String): Result<Unit> {
        return try {
            val reponse = authApi.inscrire(InscriptionRequest(email, motDePasse, nom))
            gestionnaireJeton.enregistrerSession(
                jeton = reponse.jeton,
                utilisateurId = reponse.id,
                email = reponse.email,
                nom = reponse.nom,
                role = reponse.role
            )
            Result.success(Unit)
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    fun estConnecte(): Boolean = gestionnaireJeton.estConnecte()

    fun deconnecter() = gestionnaireJeton.effacerSession()

    fun nomUtilisateur(): String? = gestionnaireJeton.obtenirNom()

    fun roleUtilisateur(): String? = gestionnaireJeton.obtenirRole()
}
