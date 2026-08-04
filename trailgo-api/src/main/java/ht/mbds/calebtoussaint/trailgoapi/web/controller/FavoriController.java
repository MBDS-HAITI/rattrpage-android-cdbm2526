// src/main/java/ht/mbds/calebtoussaint/trailgoapi/web/controller/FavoriController.java
package ht.mbds.calebtoussaint.trailgoapi.web.controller;

import ht.mbds.calebtoussaint.trailgoapi.service.FavoriService;
import ht.mbds.calebtoussaint.trailgoapi.web.dto.FavoriResponse;
import ht.mbds.calebtoussaint.trailgoapi.web.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Favoris de l'utilisateur connecte.
 *
 * Aucun identifiant d'utilisateur dans les URL : il vient toujours du
 * jeton. Sans cela, n'importe qui pourrait lire ou modifier les favoris
 * d'autrui en changeant un numero dans l'adresse.
 */
@Tag(name = "Favoris", description = "Parcours favoris de l'utilisateur connecte")
@RestController
@RequestMapping("/api/favoris")
@RequiredArgsConstructor
public class FavoriController {

    private final FavoriService favoriService;

    @Operation(summary = "Lister mes favoris")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des favoris"),
            @ApiResponse(responseCode = "403", description = "Authentification requise")
    })
    @GetMapping
    public PageResponse<FavoriResponse> mesFavoris(
            Authentication authentication,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return favoriService.listerMesFavoris(authentication.getName(), pageable);
    }

    @Operation(summary = "Ajouter un parcours aux favoris")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Favori ajoute"),
            @ApiResponse(responseCode = "409", description = "Deja en favori"),
            @ApiResponse(responseCode = "404", description = "Parcours inexistant")
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/{parcoursId}")
    public FavoriResponse ajouter(@PathVariable Long parcoursId,
                                  Authentication authentication) {
        return favoriService.ajouter(parcoursId, authentication.getName());
    }

    @Operation(summary = "Retirer un parcours des favoris")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{parcoursId}")
    public void retirer(@PathVariable Long parcoursId, Authentication authentication) {
        favoriService.retirer(parcoursId, authentication.getName());
    }

    @Operation(summary = "Verifier si un parcours est en favori",
               description = "Permet a l'interface d'afficher le coeur plein ou vide "
                           + "sans charger toute la liste.")
    @GetMapping("/{parcoursId}/statut")
    public Map<String, Boolean> statut(@PathVariable Long parcoursId,
                                       Authentication authentication) {
        return Map.of("favori",
                favoriService.estEnFavori(parcoursId, authentication.getName()));
    }
}
