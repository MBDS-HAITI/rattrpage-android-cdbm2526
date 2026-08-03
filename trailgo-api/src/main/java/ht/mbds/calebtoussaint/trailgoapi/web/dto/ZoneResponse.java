// src/main/java/ht/mbds/calebtoussaint/trailgoapi/web/dto/ZoneResponse.java
package ht.mbds.calebtoussaint.trailgoapi.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Zone geographique")
public record ZoneResponse(
        Long id,
        String nom,
        String regionAdministrative,

        @Schema(description = "Contour, directement affichable par Leaflet")
        GeoJsonPolygon polygone,

        @Schema(description = "Superficie calculee par ST_Area sur geography",
                example = "12.482")
        BigDecimal superficieKm2,

        @Schema(description = "Enveloppe [ouest, sud, est, nord]")
        List<Double> bbox,

        @Schema(description = "Nombre de parcours rattaches a cette zone")
        long nbParcours
) {}
