// src/main/java/ht/mbds/calebtoussaint/trailgoapi/domain/Parcours.java
package ht.mbds.calebtoussaint.trailgoapi.domain;

import ht.mbds.calebtoussaint.trailgoapi.domain.enums.Difficulte;
import ht.mbds.calebtoussaint.trailgoapi.domain.enums.StatutParcours;
import ht.mbds.calebtoussaint.trailgoapi.domain.enums.Theme;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** L'entite centrale du projet. */
@Entity
@Table(name = "parcours")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Parcours {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String titre;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Theme theme;

    @Column(name = "duree_estimee_min")
    private Integer dureeEstimeeMin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Difficulte difficulte;

    @Column(name = "image_couverture", length = 500)
    private String imageCouverture;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatutParcours statut = StatutParcours.BROUILLON;

    /** Le trace sur la carte, importe depuis un fichier GPX ou GeoJSON. */
    @Column(columnDefinition = "geometry(LineString,4326)")
    private LineString trace;

    /** Rectangle englobant le trace, recalcule a chaque modification. */
    @Column(columnDefinition = "geometry(Polygon,4326)")
    private Polygon bbox;

    /** Calculee par PostGIS : ST_Length(trace::geography) / 1000. */
    @Column(name = "distance_totale_km", precision = 8, scale = 3)
    private BigDecimal distanceTotaleKm;

    /**
     * FetchType.LAZY : la zone n'est chargee que si on y accede vraiment.
     * Evite de tirer un gros polygone a chaque lecture de parcours.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id")
    private ZoneGeographique zone;

    @Column(name = "nb_consultations", nullable = false)
    @Builder.Default
    private Long nbConsultations = 0L;

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private Instant dateCreation;

    @UpdateTimestamp
    @Column(name = "date_modification")
    private Instant dateModification;

    /**
     * cascade = ALL : sauvegarder un parcours sauvegarde ses etapes.
     * orphanRemoval : retirer une etape de la liste la supprime en base.
     */
    @OneToMany(mappedBy = "parcours", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordre ASC")
    @Builder.Default
    private List<Etape> etapes = new ArrayList<>();

    /** Toujours passer par ces deux methodes pour garder la relation coherente
        des deux cotes (sinon Hibernate ne voit pas le changement). */
    public void ajouterEtape(Etape etape) {
        etapes.add(etape);
        etape.setParcours(this);
    }

    public void retirerEtape(Etape etape) {
        etapes.remove(etape);
        etape.setParcours(null);
    }
}
