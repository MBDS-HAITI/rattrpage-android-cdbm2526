// src/main/java/ht/mbds/calebtoussaint/trailgoapi/web/dto/ParcoursRequest.java
package ht.mbds.calebtoussaint.trailgoapi.web.dto;

import ht.mbds.calebtoussaint.trailgoapi.domain.enums.Difficulte;
import ht.mbds.calebtoussaint.trailgoapi.domain.enums.Theme;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record ParcoursRequest(

        @NotBlank(message = "Le titre est obligatoire")
        @Size(max = 200, message = "Le titre ne peut pas depasser 200 caracteres")
        String titre,

        @Size(max = 5000, message = "La description est trop longue")
        String description,

        @NotNull(message = "Le theme est obligatoire")
        Theme theme,

        @NotNull(message = "La difficulte est obligatoire")
        Difficulte difficulte,

        @Positive(message = "La duree estimee doit etre positive")
        Integer dureeEstimeeMin,

        String imageCouverture,

        Long zoneId,

        /** @Valid en cascade : chaque etape de la liste est validee aussi. */
        @Valid
        List<EtapeRequest> etapes
) {}
