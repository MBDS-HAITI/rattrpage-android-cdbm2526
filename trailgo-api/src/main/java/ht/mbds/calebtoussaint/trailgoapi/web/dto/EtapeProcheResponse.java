// src/main/java/ht/mbds/calebtoussaint/trailgoapi/web/dto/EtapeProcheResponse.java
package ht.mbds.calebtoussaint.trailgoapi.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Etape situee a proximite du point interroge")
public record EtapeProcheResponse(
        Long id,
        String nom,
        String description,
        Double latitude,
        Double longitude,
        Integer ordre,
        String photo,
        Long parcoursId,
        String parcoursTitre,

        @Schema(description = "Distance en metres", example = "47.3")
        Double distanceM
) {}
