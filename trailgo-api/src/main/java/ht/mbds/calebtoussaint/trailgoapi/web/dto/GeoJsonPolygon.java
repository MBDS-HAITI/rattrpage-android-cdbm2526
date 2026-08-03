// src/main/java/ht/mbds/calebtoussaint/trailgoapi/web/dto/GeoJsonPolygon.java
package ht.mbds.calebtoussaint.trailgoapi.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Polygone au format GeoJSON (RFC 7946).
 *
 * TROIS NIVEAUX D'IMBRICATION, contrairement au LineString :
 *
 *   coordinates = [ anneau1, anneau2, ... ]
 *   anneau      = [ position1, position2, ... ]
 *   position    = [ longitude, latitude ]
 *
 * Le premier anneau est le contour exterieur. Les suivants, s'il y en a,
 * sont des trous (une enclave, un lac). Ici seul le contour est exploite.
 *
 * L'ANNEAU DOIT ETRE FERME : le dernier point doit etre identique au
 * premier. Sinon PostGIS refuse la geometrie.
 *
 * Exemple d'un rectangle autour du centre de Port-au-Prince :
 * {
 *   "type": "Polygon",
 *   "coordinates": [[
 *     [-72.36, 18.53], [-72.32, 18.53],
 *     [-72.32, 18.56], [-72.36, 18.56],
 *     [-72.36, 18.53]
 *   ]]
 * }
 */
@Schema(description = "Polygone GeoJSON. Coordonnees en [longitude, latitude]. "
                    + "Le premier anneau doit etre ferme.")
public record GeoJsonPolygon(

        @NotNull(message = "Le champ type est obligatoire")
        @Schema(example = "Polygon", allowableValues = {"Polygon"})
        String type,

        @NotEmpty(message = "Le tableau coordinates est obligatoire")
        @Schema(description = "Liste d'anneaux. Le premier est le contour exterieur.")
        List<List<List<Double>>> coordinates
) {}
