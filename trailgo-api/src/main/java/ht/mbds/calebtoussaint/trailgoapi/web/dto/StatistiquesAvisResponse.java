// src/main/java/ht/mbds/calebtoussaint/trailgoapi/web/dto/StatistiquesAvisResponse.java
package ht.mbds.calebtoussaint.trailgoapi.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "Statistiques des avis d'un parcours")
public record StatistiquesAvisResponse(
        Long parcoursId,

        @Schema(description = "Moyenne arrondie au dixieme", example = "4.3")
        Double noteMoyenne,

        long nbAvis,

        @Schema(description = "Nombre d'avis par note, de 1 a 5",
                example = "{\"1\":0,\"2\":1,\"3\":2,\"4\":5,\"5\":8}")
        Map<Short, Long> repartition
) {}
