// src/main/java/ht/mbds/calebtoussaint/trailgoapi/web/dto/ParcoursSummaryResponse.java
package ht.mbds.calebtoussaint.trailgoapi.web.dto;

import ht.mbds.calebtoussaint.trailgoapi.domain.enums.Difficulte;
import ht.mbds.calebtoussaint.trailgoapi.domain.enums.StatutParcours;
import ht.mbds.calebtoussaint.trailgoapi.domain.enums.Theme;

import java.math.BigDecimal;

/**
 * Version allegee pour les listes : pas d'etapes, pas de description.
 * Une liste de 20 parcours complets serait inutilement lourde,
 * surtout pour l'application mobile.
 */
public record ParcoursSummaryResponse(
        Long id,
        String titre,
        Theme theme,
        Difficulte difficulte,
        Integer dureeEstimeeMin,
        String imageCouverture,
        StatutParcours statut,
        BigDecimal distanceTotaleKm,
        Integer nbEtapes
) {}
