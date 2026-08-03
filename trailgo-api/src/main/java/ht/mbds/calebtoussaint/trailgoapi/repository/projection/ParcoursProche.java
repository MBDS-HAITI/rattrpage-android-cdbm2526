// src/main/java/ht/mbds/calebtoussaint/trailgoapi/repository/projection/ParcoursProche.java
package ht.mbds.calebtoussaint.trailgoapi.repository.projection;

import java.math.BigDecimal;

/**
 * Projection d'interface Spring Data.
 *
 * Le resultat d'une requete native est materialise directement dans
 * cette interface, sans classe intermediaire. Spring associe chaque
 * getter a la colonne de meme nom, en ignorant la casse et les
 * underscores : getDistanceM() lit la colonne "distance_m".
 */
public interface ParcoursProche {
    Long getId();
    String getTitre();
    String getTheme();
    String getDifficulte();
    Integer getDureeEstimeeMin();
    String getImageCouverture();
    BigDecimal getDistanceTotaleKm();

    /** Distance entre le point interroge et l'etape la plus proche, en metres. */
    Double getDistanceM();
}
