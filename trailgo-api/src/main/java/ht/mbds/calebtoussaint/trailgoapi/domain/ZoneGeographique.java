// src/main/java/ht/mbds/calebtoussaint/trailgoapi/domain/ZoneGeographique.java
package ht.mbds.calebtoussaint.trailgoapi.domain;

import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Polygon;

/** Zone administrative (ville, quartier, region), delimitee par un polygone. */
@Entity
@Table(name = "zone_geographique")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ZoneGeographique {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nom;

    @Column(name = "region_administrative", length = 150)
    private String regionAdministrative;

    /** Polygone JTS mappe par Hibernate Spatial vers geometry(Polygon,4326). */
    @Column(nullable = false, columnDefinition = "geometry(Polygon,4326)")
    private Polygon polygone;
}
