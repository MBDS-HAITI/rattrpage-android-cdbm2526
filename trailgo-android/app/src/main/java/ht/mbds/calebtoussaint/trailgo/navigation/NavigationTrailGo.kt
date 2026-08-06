// navigation/NavigationTrailGo.kt
package ht.mbds.calebtoussaint.trailgo.navigation

/**
 * Noms des routes de navigation, centralises pour eviter les fautes
 * de frappe dans des chaines eparpillees dans le code (equivalent des
 * chemins declares dans App.jsx cote React).
 */
object Routes {
    const val CONNEXION = "connexion"
    const val LISTE_PARCOURS = "liste_parcours"
    const val DETAIL_PARCOURS = "detail_parcours/{parcoursId}"

    fun detailParcours(id: Long) = "detail_parcours/$id"
}
