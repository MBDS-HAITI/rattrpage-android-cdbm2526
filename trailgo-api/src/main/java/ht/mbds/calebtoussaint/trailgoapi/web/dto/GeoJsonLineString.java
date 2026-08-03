// src/main/java/ht/mbds/calebtoussaint/trailgoapi/web/dto/GeoJsonLineString.java
package ht.mbds.calebtoussaint.trailgoapi.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Geometrie LineString au format GeoJSON (RFC 7946).
 *
 * ATTENTION A L'ORDRE DES COORDONNEES : la RFC 7946 impose
 * [longitude, latitude], donc l'inverse de la facon dont on enonce
 * habituellement une position. Une inversion place un parcours de
 * Port-au-Prince (18.54, -72.33) au large de la Somalie (-72.33, 18.54).
 *
 * Exemple de charge utile acceptee :
 * {
 *   "type": "LineString",
 *   "coordinates": [[-72.3395, 18.5479], [-72.3378, 18.5426]]
 * }
 */
@Schema(description = "Trace au format GeoJSON. Coordonnees en [longitude, latitude].")
public record GeoJsonLineString(

        @NotNull(message = "Le champ type est obligatoire")
        @Schema(example = "LineString", allowableValues = {"LineString"})
        String type,

        @NotEmpty(message = "Le tableau coordinates est obligatoire")
        @Schema(description = "Liste de paires [longitude, latitude]",
                example = "[[-72.3395, 18.5479], [-72.3378, 18.5426]]")
        List<List<Double>> coordinates
) {}
