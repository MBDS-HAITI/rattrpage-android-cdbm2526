// src/main/java/ht/mbds/calebtoussaint/trailgoapi/web/dto/TraceResponse.java
package ht.mbds.calebtoussaint.trailgoapi.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/**
 * Trace d'un parcours, pret a etre consomme par Leaflet (React) ou
 * par une Polyline Google Maps (Android).
 */
@Schema(description = "Trace cartographique d'un parcours")
public record TraceResponse(

        Long parcoursId,

        @Schema(description = "Geometrie GeoJSON, directement affichable par Leaflet")
        GeoJsonLineString geometrie,

        @Schema(description = "Distance calculee par PostGIS (ST_Length sur geography)",
                example = "3.472")
        BigDecimal distanceKm,

        @Schema(description = "Nombre de points composant le trace")
        int nbPoints,

        @Schema(description = "Enveloppe [ouest, sud, est, nord], utile pour "
                            + "centrer automatiquement la carte")
        List<Double> bbox
) {}
