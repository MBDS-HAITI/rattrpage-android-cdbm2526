// src/main/java/ht/mbds/calebtoussaint/trailgoapi/web/dto/ConnexionRequest.java
package ht.mbds.calebtoussaint.trailgoapi.web.dto;

import jakarta.validation.constraints.NotBlank;

public record ConnexionRequest(

        @NotBlank(message = "L'email est obligatoire")
        String email,

        @NotBlank(message = "Le mot de passe est obligatoire")
        String motDePasse
) {}
