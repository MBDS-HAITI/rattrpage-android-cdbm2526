// src/main/java/ht/mbds/calebtoussaint/trailgoapi/domain/Avis.java
package ht.mbds.calebtoussaint.trailgoapi.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/** Note et commentaire laisses par un touriste sur un parcours. */
@Entity
@Table(name = "avis")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Avis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parcours_id", nullable = false)
    private Parcours parcours;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auteur_id", nullable = false)
    private Utilisateur auteur;

    /** Entre 1 et 5. Short car la colonne est un SMALLINT. */
    @Column(nullable = false)
    private Short note;

    @Column(columnDefinition = "text")
    private String commentaire;

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private Instant dateCreation;

    /** Signale par un utilisateur : apparait dans la moderation du back office. */
    @Column(nullable = false)
    @Builder.Default
    private boolean signale = false;
}
