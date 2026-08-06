// data/model/Parcours.kt
package ht.mbds.calebtoussaint.trailgo.data.model

/**
 * Modeles de donnees des parcours.
 * Miroir Kotlin des DTO Java de l'API (ParcoursResponse, EtapeResponse...).
 *
 * Les champs nullables (String? plutot que String) correspondent aux
 * champs que l'API peut renvoyer a null (description, imageCouverture...).
 * Le "?" en Kotlin force a gerer explicitement l'absence de valeur,
 * ce qui evite les NullPointerException a l'execution.
 */

data class EtapeResponse(
    val id: Long,
    val nom: String,
    val description: String?,
    val latitude: Double,
    val longitude: Double,
    val ordre: Int,
    val photo: String?,
    val dureeVisiteMin: Int?
)

data class ParcoursResponse(
    val id: Long,
    val titre: String,
    val description: String?,
    val theme: String,
    val difficulte: String,
    val dureeEstimeeMin: Int?,
    val imageCouverture: String?,
    val statut: String,
    val distanceTotaleKm: Double?,
    val zoneId: Long?,
    val zoneNom: String?,
    val nbConsultations: Long,
    val dateCreation: String,
    val dateModification: String?,
    val etapes: List<EtapeResponse>
)

data class ParcoursSummaryResponse(
    val id: Long,
    val titre: String,
    val theme: String,
    val difficulte: String,
    val dureeEstimeeMin: Int?,
    val imageCouverture: String?,
    val statut: String,
    val distanceTotaleKm: Double?,
    val nbEtapes: Int
)

/**
 * Enveloppe de pagination, identique a celle de l'API.
 * Le "<T>" (generique) permet de reutiliser cette classe pour paginer
 * n'importe quel type de contenu : parcours, avis, utilisateurs...
 */
data class PageResponse<T>(
    val contenu: List<T>,
    val page: Int,
    val taille: Int,
    val totalElements: Long,
    val totalPages: Int,
    val premiere: Boolean,
    val derniere: Boolean
)
