// src/main/java/ht/mbds/calebtoussaint/trailgoapi/web/dto/ZoneRequest.java
package ht.mbds.calebtoussaint.trailgoapi.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ZoneRequest(

        @NotBlank(message = "Le nom de la zone est obligatoire")
        @Size(max = 150)
        String nom,

        @Size(max = 150)
        String regionAdministrative,

        @NotNull(message = "Le polygone est obligatoire")
        @Valid
        GeoJsonPolygon polygone
) {}
