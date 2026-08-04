// src/main/java/ht/mbds/calebtoussaint/trailgoapi/web/controller/AvisController.java
package ht.mbds.calebtoussaint.trailgoapi.web.controller;

import ht.mbds.calebtoussaint.trailgoapi.service.AvisService;
import ht.mbds.calebtoussaint.trailgoapi.web.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Avis", description = "Notes et commentaires des touristes")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AvisController {

    private final AvisService avisService;

    // ---------- Lecture publique ----------

    @Operation(summary = "Lister les avis d'un parcours",
               description = "Accessible sans authentification.")
    @GetMapping("/parcours/{parcoursId}/avis")
    public PageResponse<AvisResponse> lister(
            @PathVariable Long parcoursId,
            @ParameterObject
            @PageableDefault(size = 20, sort = "dateCreation",
                             direction = Sort.Direction.DESC) Pageable pageable) {
        return avisService.listerParParcours(parcoursId, pageable);
    }

    @Operation(summary = "Statistiques des avis d'un parcours",
               description = "Note moyenne, nombre d'avis et repartition de 1 a 5. "
                           + "Alimente le tableau de bord du back office.")
    @GetMapping("/parcours/{parcoursId}/avis/statistiques")
    public StatistiquesAvisResponse statistiques(@PathVariable Long parcoursId) {
        return avisService.statistiques(parcoursId);
    }

    // ---------- Ecriture par un utilisateur connecte ----------

    @Operation(summary = "Deposer un avis",
               description = """
                       Un utilisateur ne peut deposer qu'un seul avis par parcours,
                       et uniquement sur un parcours publie.
                       """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Avis enregistre"),
            @ApiResponse(responseCode = "400", description = "Note hors bornes 1-5"),
            @ApiResponse(responseCode = "409", description = "Avis deja depose, "
                                                          + "ou parcours non publie"),
            @ApiResponse(responseCode = "403", description = "Authentification requise")
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/parcours/{parcoursId}/avis")
    public AvisResponse deposer(@PathVariable Long parcoursId,
                                @Valid @RequestBody AvisRequest requete,
                                Authentication authentication) {
        return avisService.deposer(parcoursId, requete, authentication.getName());
    }

    @Operation(summary = "Modifier son avis",
               description = "Seul l'auteur peut modifier son avis. Un administrateur "
                           + "peut le supprimer mais pas le reecrire.")
    @PutMapping("/avis/{avisId}")
    public AvisResponse modifier(@PathVariable Long avisId,
                                 @Valid @RequestBody AvisRequest requete,
                                 Authentication authentication) {
        return avisService.modifier(avisId, requete, authentication.getName());
    }

    @Operation(summary = "Supprimer un avis",
               description = "Autorise a l'auteur de l'avis ou a un administrateur.")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/avis/{avisId}")
    public void supprimer(@PathVariable Long avisId, Authentication authentication) {
        avisService.supprimer(avisId, authentication.getName());
    }

    @Operation(summary = "Signaler un avis",
               description = "L'avis remonte dans la file de moderation du back office.")
    @PostMapping("/avis/{avisId}/signalement")
    public AvisResponse signaler(@PathVariable Long avisId) {
        return avisService.signaler(avisId);
    }

    // ---------- Moderation, reservee aux ADMIN ----------

    @Operation(summary = "Lister les avis signales",
               description = "Ecran de moderation. Reserve aux administrateurs.")
    @GetMapping("/avis/signales")
    public PageResponse<AvisResponse> listerSignales(
            @ParameterObject
            @PageableDefault(size = 20, sort = "dateCreation",
                             direction = Sort.Direction.DESC) Pageable pageable) {
        return avisService.listerSignales(pageable);
    }

    @Operation(summary = "Lever un signalement",
               description = "L'avis est juge acceptable et sort de la moderation.")
    @DeleteMapping("/avis/{avisId}/signalement")
    public AvisResponse leverSignalement(@PathVariable Long avisId) {
        return avisService.leverSignalement(avisId);
    }
}
