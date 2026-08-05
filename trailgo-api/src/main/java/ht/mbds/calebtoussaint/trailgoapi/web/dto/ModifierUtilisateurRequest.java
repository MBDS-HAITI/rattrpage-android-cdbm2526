// src/main/java/ht/mbds/calebtoussaint/trailgoapi/web/dto/ModifierUtilisateurRequest.java
package ht.mbds.calebtoussaint.trailgoapi.web.dto;

import ht.mbds.calebtoussaint.trailgoapi.domain.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Modification d'un compte par un administrateur.
 * Les deux champs sont obligatoires : le client renvoie toujours
 * l'etat complet souhaite, pas une modification partielle.
 */
@Schema(description = "Nouveau role et statut actif d'un utilisateur")
public record ModifierUtilisateurRequest(

        @NotNull(message = "Le role est obligatoire")
        Role role,

        @NotNull(message = "Le statut actif est obligatoire")
        Boolean actif
) {}
