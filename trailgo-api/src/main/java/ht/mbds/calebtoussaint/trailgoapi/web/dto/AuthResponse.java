// src/main/java/ht/mbds/calebtoussaint/trailgoapi/web/dto/AuthResponse.java
package ht.mbds.calebtoussaint.trailgoapi.web.dto;

import ht.mbds.calebtoussaint.trailgoapi.domain.enums.Role;

/**
 * Reponse de connexion et d'inscription.
 * Le mot de passe n'y figure evidemment jamais.
 */
public record AuthResponse(
        String jeton,
        String typeJeton,
        Long id,
        String email,
        String nom,
        Role role,
        long expireDansSecondes
) {}
