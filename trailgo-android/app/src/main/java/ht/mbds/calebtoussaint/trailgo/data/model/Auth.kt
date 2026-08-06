// data/model/Auth.kt
package ht.mbds.calebtoussaint.trailgo.data.model

/**
 * Modeles de donnees pour l'authentification.
 *
 * En Kotlin, une "data class" est l'equivalent d'un record Java :
 * constructeur, equals(), hashCode() et toString() sont generes
 * automatiquement a partir des proprietes declarees.
 */

data class ConnexionRequest(
    val email: String,
    val motDePasse: String
)

data class InscriptionRequest(
    val email: String,
    val motDePasse: String,
    val nom: String
)

/**
 * Reponse de connexion et d'inscription.
 * Correspond exactement au AuthResponse renvoye par l'API Spring Boot.
 */
data class AuthResponse(
    val jeton: String,
    val typeJeton: String,
    val id: Long,
    val email: String,
    val nom: String,
    val role: String,
    val expireDansSecondes: Long
)

data class UtilisateurResponse(
    val id: Long,
    val email: String,
    val nom: String,
    val role: String,
    val actif: Boolean,
    val dateCreation: String
)
