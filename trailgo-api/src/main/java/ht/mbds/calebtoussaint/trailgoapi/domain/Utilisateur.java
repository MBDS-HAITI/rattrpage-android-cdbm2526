// src/main/java/ht/mbds/calebtoussaint/trailgoapi/domain/Utilisateur.java
package ht.mbds.calebtoussaint.trailgoapi.domain;

import ht.mbds.calebtoussaint.trailgoapi.domain.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "utilisateur")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 180)
    private String email;

    /** Toujours un hash BCrypt, jamais le mot de passe en clair. */
    @Column(name = "mot_de_passe", nullable = false, length = 120)
    private String motDePasse;

    @Column(length = 120)
    private String nom;

    /** EnumType.STRING : stocke "ADMIN" et non 0. Indispensable ici,
        sinon ajouter un role plus tard decalerait tout. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false)
    @Builder.Default
    private boolean actif = true;

    /** Rempli automatiquement par Hibernate a l'insertion. */
    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private Instant dateCreation;
}
