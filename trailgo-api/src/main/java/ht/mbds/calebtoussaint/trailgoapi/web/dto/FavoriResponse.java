// src/main/java/ht/mbds/calebtoussaint/trailgoapi/web/dto/FavoriResponse.java
package ht.mbds.calebtoussaint.trailgoapi.web.dto;

import java.time.Instant;

public record FavoriResponse(
        ParcoursSummaryResponse parcours,
        Instant dateAjout
) {}
