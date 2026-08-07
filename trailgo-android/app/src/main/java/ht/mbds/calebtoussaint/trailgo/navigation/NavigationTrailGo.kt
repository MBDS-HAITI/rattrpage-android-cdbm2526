// navigation/NavigationTrailGo.kt
package ht.mbds.calebtoussaint.trailgo.navigation

object Routes {
    const val CONNEXION = "connexion"
    const val LISTE_PARCOURS = "liste_parcours"
    const val DETAIL_PARCOURS = "detail_parcours/{parcoursId}"
    const val CARTE_PARCOURS = "carte_parcours/{parcoursId}"

    fun detailParcours(id: Long) = "detail_parcours/$id"
    fun carteParcours(id: Long) = "carte_parcours/$id"
}