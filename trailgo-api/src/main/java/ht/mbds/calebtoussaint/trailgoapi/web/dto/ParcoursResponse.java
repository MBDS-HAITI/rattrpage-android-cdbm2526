// src/main/java/ht/mbds/calebtoussaint/trailgoapi/web/dto/ParcoursResponse.java
package ht.mbds.calebtoussaint.trailgoapi.web.dto;

import ht.mbds.calebtoussaint.trailgoapi.domain.enums.Difficulte;
import ht.mbds.calebtoussaint.trailgoapi.domain.enums.StatutParcours;
import ht.mbds.calebtoussaint.trailgoapi.domain.enums.Theme;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Fiche complete d'un parcours, avec ses etapes. */
public record ParcoursResponse(
        Long id,
        String titre,
        String description,
        Theme theme,
        Difficulte difficulte,
        Integer dureeEstimeeMin,
        String imageCouverture,
        StatutParcours statut,
        BigDecimal distanceTotaleKm,
        Long zoneId,
        String zoneNom,
        Long nbConsultations,
        Instant dateCreation,
        Instant dateModification,
        List<EtapeResponse> etapes
) {}
