// src/main/java/ht/mbds/calebtoussaint/trailgoapi/web/dto/ParcoursProcheResponse.java
package ht.mbds.calebtoussaint.trailgoapi.web.dto;

import ht.mbds.calebtoussaint.trailgoapi.domain.enums.Difficulte;
import ht.mbds.calebtoussaint.trailgoapi.domain.enums.Theme;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Parcours trouve par recherche spatiale")
public record ParcoursProcheResponse(
        Long id,
        String titre,
        Theme theme,
        Difficulte difficulte,
        Integer dureeEstimeeMin,
        String imageCouverture,

        @Schema(description = "Longueur totale du parcours en km")
        BigDecimal distanceTotaleKm,

        @Schema(description = "Distance entre le point interroge et l'etape la "
                            + "plus proche, en metres. Absent pour une recherche "
                            + "par rectangle.",
                example = "342.7")
        Double distanceM
) {}
