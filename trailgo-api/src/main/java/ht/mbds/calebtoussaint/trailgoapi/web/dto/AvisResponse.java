// src/main/java/ht/mbds/calebtoussaint/trailgoapi/web/dto/AvisResponse.java
package ht.mbds.calebtoussaint.trailgoapi.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Avis laisse par un touriste")
public record AvisResponse(
        Long id,
        Long parcoursId,
        String parcoursTitre,
        Short note,
        String commentaire,

        @Schema(description = "Nom de l'auteur. L'email n'est jamais expose.")
        String auteurNom,

        Long auteurId,
        Instant dateCreation,

        @Schema(description = "Avis signale, en attente de moderation")
        boolean signale
) {}
