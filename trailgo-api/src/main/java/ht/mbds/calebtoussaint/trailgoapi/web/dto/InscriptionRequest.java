// src/main/java/ht/mbds/calebtoussaint/trailgoapi/web/dto/InscriptionRequest.java
package ht.mbds.calebtoussaint.trailgoapi.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InscriptionRequest(

        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "Format d'email invalide")
        @Size(max = 180)
        String email,

        @NotBlank(message = "Le mot de passe est obligatoire")
        @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caracteres")
        String motDePasse,

        @NotBlank(message = "Le nom est obligatoire")
        @Size(max = 120)
        String nom
) {}
