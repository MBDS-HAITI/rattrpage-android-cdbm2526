// src/main/java/ht/mbds/calebtoussaint/trailgoapi/web/controller/ParcoursController.java
package ht.mbds.calebtoussaint.trailgoapi.web.controller;

import ht.mbds.calebtoussaint.trailgoapi.domain.enums.Difficulte;
import ht.mbds.calebtoussaint.trailgoapi.domain.enums.StatutParcours;
import ht.mbds.calebtoussaint.trailgoapi.domain.enums.Theme;
import ht.mbds.calebtoussaint.trailgoapi.service.ParcoursService;
import ht.mbds.calebtoussaint.trailgoapi.web.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * Point d'entree HTTP pour les parcours.
 *
 * Le controleur reste mince : il recoit, delegue au service, et renvoie.
 * Aucune logique metier ici.
 *
 * Les annotations @Tag, @Operation et @ApiResponse alimentent la
 * documentation Swagger, exigee par le sujet.
 */
@Tag(name = "Parcours",
        description = "Consultation et administration des parcours touristiques")
@RestController
@RequestMapping("/api/parcours")
@RequiredArgsConstructor
public class ParcoursController {

    private final ParcoursService parcoursService;

    /**
     * GET /api/parcours
     *
     * @ParameterObject (springdoc) est ESSENTIEL ici : sans lui, Swagger
     * affiche le Pageable comme un objet JSON a remplir a la main, ce qui
     * produit des requetes invalides du type sort=["string"].
     * Avec l'annotation, on obtient trois champs distincts page, size et
     * sort, correctement types.
     */
    @Operation(summary = "Lister les parcours",
            description = """
                       Liste paginee, triable et filtrable.
                       Exemples de tri : ?sort=titre,asc ou ?sort=dateCreation,desc
                       Recherche sur le titre et la description via ?recherche=
                       """)
    @GetMapping
    public PageResponse<ParcoursSummaryResponse> lister(

            @Parameter(description = "Filtrer par theme")
            @RequestParam(required = false) Theme theme,

            @Parameter(description = "Filtrer par difficulte")
            @RequestParam(required = false) Difficulte difficulte,

            @Parameter(description = "Filtrer par statut (BROUILLON ou PUBLIE)")
            @RequestParam(required = false) StatutParcours statut,

            @Parameter(description = "Duree maximale en minutes")
            @RequestParam(required = false) Integer dureeMax,

            @Parameter(description = "Termes recherches dans le titre et la description")
            @RequestParam(required = false) String recherche,

            @ParameterObject
            @PageableDefault(size = 20, sort = "dateCreation",
                    direction = Sort.Direction.DESC) Pageable pageable) {

        return parcoursService.rechercher(theme, difficulte, statut,
                dureeMax, recherche, pageable);
    }

    @Operation(summary = "Consulter un parcours",
            description = "Incremente le compteur de consultations "
                    + "utilise par le tableau de bord.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Parcours trouve"),
            @ApiResponse(responseCode = "404", description = "Parcours inexistant")
    })
    @GetMapping("/{id}")
    public ParcoursResponse consulter(@PathVariable Long id) {
        return parcoursService.consulterEtComptabiliser(id);
    }

    /**
     * POST /api/parcours
     *
     * @Valid declenche la verification des annotations du DTO. En cas
     * d'echec, le GestionnaireExceptions renvoie un 400 detaille.
     *
     * On renvoie 201 Created avec l'en-tete Location : c'est la reponse
     * attendue d'une API REST correcte apres une creation.
     */
    @Operation(summary = "Creer un parcours",
            description = "Le parcours est toujours cree au statut BROUILLON.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Parcours cree"),
            @ApiResponse(responseCode = "400", description = "Donnees invalides")
    })
    @PostMapping
    public ResponseEntity<ParcoursResponse> creer(@Valid @RequestBody ParcoursRequest requete,
                                                  UriComponentsBuilder uriBuilder) {
        ParcoursResponse cree = parcoursService.creer(requete);
        URI localisation = uriBuilder.path("/api/parcours/{id}")
                .buildAndExpand(cree.id()).toUri();
        return ResponseEntity.created(localisation).body(cree);
    }

    @Operation(summary = "Modifier un parcours")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Parcours modifie"),
            @ApiResponse(responseCode = "400", description = "Donnees invalides"),
            @ApiResponse(responseCode = "404", description = "Parcours inexistant")
    })
    @PutMapping("/{id}")
    public ParcoursResponse modifier(@PathVariable Long id,
                                     @Valid @RequestBody ParcoursRequest requete) {
        return parcoursService.modifier(id, requete);
    }

    @Operation(summary = "Publier un parcours",
            description = "Refuse avec un 409 si le parcours n'a aucune etape.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Parcours publie"),
            @ApiResponse(responseCode = "409", description = "Parcours sans etape"),
            @ApiResponse(responseCode = "404", description = "Parcours inexistant")
    })
    @PostMapping("/{id}/publication")
    public ParcoursResponse publier(@PathVariable Long id) {
        return parcoursService.changerPublication(id, true);
    }

    @Operation(summary = "Depublier un parcours",
            description = "Repasse le parcours en BROUILLON.")
    @DeleteMapping("/{id}/publication")
    public ParcoursResponse depublier(@PathVariable Long id) {
        return parcoursService.changerPublication(id, false);
    }

    @Operation(summary = "Supprimer un parcours",
            description = "Supprime aussi ses etapes (suppression en cascade).")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Parcours supprime"),
            @ApiResponse(responseCode = "404", description = "Parcours inexistant")
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    public void supprimer(@PathVariable Long id) {
        parcoursService.supprimer(id);
    }
}