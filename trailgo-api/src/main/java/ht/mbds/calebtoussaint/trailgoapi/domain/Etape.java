// src/main/java/ht/mbds/calebtoussaint/trailgoapi/domain/Etape.java
package ht.mbds.calebtoussaint.trailgoapi.domain;

import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;

/** Un point d'arret du parcours, avec un ordre de visite. */
@Entity
@Table(name = "etape")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Etape {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parcours_id", nullable = false)
    private Parcours parcours;

    @Column(nullable = false, length = 200)
    private String nom;

    @Column(columnDefinition = "text")
    private String description;

    /**
     * Position GPS.
     * RAPPEL : position.getY() = latitude, position.getX() = longitude.
     * Pour creer un point, utiliser GeoUtils.point(lat, lng).
     */
    @Column(nullable = false, columnDefinition = "geometry(Point,4326)")
    private Point position;

    /** Rang dans le parcours, a partir de 1. */
    @Column(nullable = false)
    private Integer ordre;

    @Column(length = 500)
    private String photo;

    @Column(name = "duree_visite_min")
    private Integer dureeVisiteMin;
}
