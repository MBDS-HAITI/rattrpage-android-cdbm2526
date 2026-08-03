// src/main/java/ht/mbds/calebtoussaint/trailgoapi/web/dto/PoiProcheResponse.java
package ht.mbds.calebtoussaint.trailgoapi.web.dto;

import ht.mbds.calebtoussaint.trailgoapi.domain.enums.CategoriePoi;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Point d'interet situe a proximite du point interroge")
public record PoiProcheResponse(
        Long id,
        String titre,
        CategoriePoi categorie,
        String adresse,
        Double latitude,
        Double longitude,
        Integer rayonProximiteM,

        @Schema(description = "Distance en metres", example = "128.4")
        Double distanceM
) {}
