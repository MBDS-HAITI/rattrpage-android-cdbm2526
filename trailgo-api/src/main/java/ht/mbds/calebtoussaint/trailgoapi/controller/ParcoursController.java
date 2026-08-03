// src/main/java/ht/mbds/calebtoussaint/trailgoapi/web/controller/ParcoursController.java
package ht.mbds.calebtoussaint.trailgoapi.controller;

import ht.mbds.calebtoussaint.trailgoapi.domain.enums.Difficulte;
import ht.mbds.calebtoussaint.trailgoapi.domain.enums.StatutParcours;
import ht.mbds.calebtoussaint.trailgoapi.domain.enums.Theme;
import ht.mbds.calebtoussaint.trailgoapi.service.ParcoursService;
import ht.mbds.calebtoussaint.trailgoapi.web.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
 */
@RestController
@RequestMapping("/api/parcours")
@RequiredArgsConstructor
public class ParcoursController {

    private final ParcoursService parcoursService;

    /**
     * GET /api/parcours
     *
     * Exemples :
     *   /api/parcours?theme=HISTORIQUE
     *   /api/parcours?recherche=port&page=0&size=5
     *   /api/parcours?sort=titre,asc
     *
     * Spring construit l'objet Pageable tout seul a partir des parametres
     * page, size et sort.
     */
    @GetMapping
    public PageResponse<ParcoursSummaryResponse> lister(
            @RequestParam(required = false) Theme theme,
            @RequestParam(required = false) Difficulte difficulte,
            @RequestParam(required = false) StatutParcours statut,
            @RequestParam(required = false) Integer dureeMax,
            @RequestParam(required = false) String recherche,
            @PageableDefault(size = 20, sort = "dateCreation",
                             direction = Sort.Direction.DESC) Pageable pageable) {

        return parcoursService.rechercher(theme, difficulte, statut,
                                          dureeMax, recherche, pageable);
    }

    /** GET /api/parcours/{id} */
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
    @PostMapping
    public ResponseEntity<ParcoursResponse> creer(@Valid @RequestBody ParcoursRequest requete,
                                                  UriComponentsBuilder uriBuilder) {
        ParcoursResponse cree = parcoursService.creer(requete);
        URI localisation = uriBuilder.path("/api/parcours/{id}")
                                     .buildAndExpand(cree.id()).toUri();
        return ResponseEntity.created(localisation).body(cree);
    }

    /** PUT /api/parcours/{id} */
    @PutMapping("/{id}")
    public ParcoursResponse modifier(@PathVariable Long id,
                                     @Valid @RequestBody ParcoursRequest requete) {
        return parcoursService.modifier(id, requete);
    }

    /** POST /api/parcours/{id}/publication */
    @PostMapping("/{id}/publication")
    public ParcoursResponse publier(@PathVariable Long id) {
        return parcoursService.changerPublication(id, true);
    }

    /** DELETE /api/parcours/{id}/publication */
    @DeleteMapping("/{id}/publication")
    public ParcoursResponse depublier(@PathVariable Long id) {
        return parcoursService.changerPublication(id, false);
    }

    /** DELETE /api/parcours/{id} : 204 No Content, sans corps de reponse. */
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    public void supprimer(@PathVariable Long id) {
        parcoursService.supprimer(id);
    }
}
