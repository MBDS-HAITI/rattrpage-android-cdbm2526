// src/main/java/ht/mbds/calebtoussaint/trailgoapi/web/dto/EtapeRequest.java
package ht.mbds.calebtoussaint.trailgoapi.web.dto;

import jakarta.validation.constraints.*;

/**
 * Donnees recues du client pour creer ou modifier une etape.
 *
 * "record" est une classe Java immuable en une ligne : les champs, le
 * constructeur, equals(), hashCode() et toString() sont generes.
 *
 * Les annotations @NotBlank, @DecimalMin... sont verifiees
 * automatiquement grace au @Valid pose dans le controleur.
 */
public record EtapeRequest(

        @NotBlank(message = "Le nom de l'etape est obligatoire")
        @Size(max = 200, message = "Le nom ne peut pas depasser 200 caracteres")
        String nom,

        String description,

        @NotNull(message = "La latitude est obligatoire")
        @DecimalMin(value = "-90.0", message = "La latitude doit etre comprise entre -90 et 90")
        @DecimalMax(value = "90.0",  message = "La latitude doit etre comprise entre -90 et 90")
        Double latitude,

        @NotNull(message = "La longitude est obligatoire")
        @DecimalMin(value = "-180.0", message = "La longitude doit etre comprise entre -180 et 180")
        @DecimalMax(value = "180.0",  message = "La longitude doit etre comprise entre -180 et 180")
        Double longitude,

        String photo,

        @Positive(message = "La duree de visite doit etre positive")
        Integer dureeVisiteMin
) {}
