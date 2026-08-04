// src/main/java/ht/mbds/calebtoussaint/trailgoapi/web/dto/AvisRequest.java
package ht.mbds.calebtoussaint.trailgoapi.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AvisRequest(

        @NotNull(message = "La note est obligatoire")
        @Min(value = 1, message = "La note minimale est 1")
        @Max(value = 5, message = "La note maximale est 5")
        @Schema(example = "4")
        Short note,

        @Size(max = 2000, message = "Le commentaire ne peut pas depasser 2000 caracteres")
        @Schema(example = "Parcours tres agreable, bien balise.")
        String commentaire
) {}
