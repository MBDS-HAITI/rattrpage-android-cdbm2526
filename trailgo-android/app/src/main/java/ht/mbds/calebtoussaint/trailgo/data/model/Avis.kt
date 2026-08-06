// data/model/Avis.kt
package ht.mbds.calebtoussaint.trailgo.data.model

data class AvisRequest(
    val note: Int,
    val commentaire: String?
)

data class AvisResponse(
    val id: Long,
    val parcoursId: Long,
    val parcoursTitre: String,
    val note: Int,
    val commentaire: String?,
    val auteurNom: String,
    val auteurId: Long,
    val dateCreation: String,
    val signale: Boolean
)

data class FavoriResponse(
    val parcours: ParcoursSummaryResponse,
    val dateAjout: String
)
