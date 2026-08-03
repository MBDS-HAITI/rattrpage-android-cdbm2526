// src/main/java/ht/mbds/calebtoussaint/trailgoapi/web/dto/UtilisateurResponse.java
package ht.mbds.calebtoussaint.trailgoapi.web.dto;

import ht.mbds.calebtoussaint.trailgoapi.domain.enums.Role;

import java.time.Instant;

public record UtilisateurResponse(
        Long id,
        String email,
        String nom,
        Role role,
        boolean actif,
        Instant dateCreation
) {}
