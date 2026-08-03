// src/main/java/ht/mbds/calebtoussaint/trailgoapi/domain/Favori.java
package ht.mbds.calebtoussaint.trailgoapi.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Liaison utilisateur <-> parcours.
 *
 * Particularite : la cle primaire est composee de DEUX colonnes
 * (utilisateur_id, parcours_id). En JPA cela se represente avec une
 * classe @Embeddable dediee, declaree en bas de ce fichier.
 */
@Entity
@Table(name = "favori")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Favori {

    @EmbeddedId
    private FavoriId id;

    /** @MapsId : cette relation alimente le champ correspondant de la cle. */
    @MapsId("utilisateurId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;

    @MapsId("parcoursId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parcours_id")
    private Parcours parcours;

    @CreationTimestamp
    @Column(name = "date_ajout", nullable = false, updatable = false)
    private Instant dateAjout;

    /**
     * Cle composite. equals() et hashCode() sont OBLIGATOIRES ici :
     * Hibernate s'en sert pour identifier les lignes. Sans eux, on obtient
     * des doublons et des comportements incomprehensibles.
     */
    @Embeddable
    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    public static class FavoriId implements Serializable {

        @Column(name = "utilisateur_id")
        private Long utilisateurId;

        @Column(name = "parcours_id")
        private Long parcoursId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof FavoriId autre)) return false;
            return Objects.equals(utilisateurId, autre.utilisateurId)
                && Objects.equals(parcoursId, autre.parcoursId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(utilisateurId, parcoursId);
        }
    }
}
