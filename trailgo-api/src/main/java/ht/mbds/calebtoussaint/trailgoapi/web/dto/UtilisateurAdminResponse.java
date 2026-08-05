// src/main/java/ht/mbds/calebtoussaint/trailgoapi/web/dto/UtilisateurAdminResponse.java
package ht.mbds.calebtoussaint.trailgoapi.web.dto;

import ht.mbds.calebtoussaint.trailgoapi.domain.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Vue d'un utilisateur reservee a l'administration.
 * L'email est expose ici (utile pour identifier un compte), a la
 * difference du DTO utilise dans les avis publics.
 */
@Schema(description = "Compte utilisateur, vue administration")
public record UtilisateurAdminResponse(
        Long id,
        String email,
        String nom,
        Role role,
        boolean actif,
        Instant dateCreation
) {}
