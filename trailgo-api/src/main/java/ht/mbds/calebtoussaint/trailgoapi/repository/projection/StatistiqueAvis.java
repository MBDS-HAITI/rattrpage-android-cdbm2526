// src/main/java/ht/mbds/calebtoussaint/trailgoapi/repository/projection/StatistiqueAvis.java
package ht.mbds.calebtoussaint.trailgoapi.repository.projection;

/**
 * Resultat de l'agregation des notes d'un parcours.
 * Projection d'interface : pas de classe intermediaire a ecrire.
 */
public interface StatistiqueAvis {
    Long getParcoursId();
    Double getMoyenne();
    Long getTotal();
}
