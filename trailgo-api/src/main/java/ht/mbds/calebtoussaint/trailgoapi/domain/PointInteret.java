// src/main/java/ht/mbds/calebtoussaint/trailgoapi/domain/PointInteret.java
package ht.mbds.calebtoussaint.trailgoapi.domain;

import ht.mbds.calebtoussaint.trailgoapi.domain.enums.CategoriePoi;
import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;

/** Lieu remarquable, independant des parcours. Sert aux alertes de proximite. */
@Entity
@Table(name = "point_interet")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PointInteret {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String titre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CategoriePoi categorie;

    @Column(length = 300)
    private String adresse;

    @Column(nullable = false, columnDefinition = "geometry(Point,4326)")
    private Point position;

    /** Distance de declenchement de l'alerte, en metres. */
    @Column(name = "rayon_proximite_m", nullable = false)
    @Builder.Default
    private Integer rayonProximiteM = 200;
}
