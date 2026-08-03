// src/main/java/ht/mbds/calebtoussaint/trailgoapi/web/controller/ZoneController.java
package ht.mbds.calebtoussaint.trailgoapi.web.controller;

import ht.mbds.calebtoussaint.trailgoapi.service.ZoneService;
import ht.mbds.calebtoussaint.trailgoapi.web.dto.ParcoursSummaryResponse;
import ht.mbds.calebtoussaint.trailgoapi.web.dto.ZoneRequest;
import ht.mbds.calebtoussaint.trailgoapi.web.dto.ZoneResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Tag(name = "Zones geographiques",
     description = "Decoupage territorial et rattachement automatique des parcours")
@RestController
@RequestMapping("/api/zones")
@RequiredArgsConstructor
public class ZoneController {

    private final ZoneService zoneService;

    @Operation(summary = "Lister les zones",
               description = "Chaque zone est renvoyee avec son contour GeoJSON, "
                           + "sa superficie et le nombre de parcours qu'elle contient.")
    @GetMapping
    public List<ZoneResponse> lister() {
        return zoneService.lister();
    }

    @Operation(summary = "Consulter une zone")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Zone trouvee"),
            @ApiResponse(responseCode = "404", description = "Zone inexistante")
    })
    @GetMapping("/{id}")
    public ZoneResponse consulter(@PathVariable Long id) {
        return zoneService.consulter(id);
    }

    @Operation(summary = "Parcours contenus dans une zone",
               description = """
                       Renvoie les parcours dont le trace est ENTIEREMENT inclus
                       dans le polygone de la zone (ST_Within).

                       A distinguer de la recherche par rectangle, qui utilise
                       ST_Intersects et renvoie aussi les parcours qui ne font
                       que traverser la zone.
                       """)
    @GetMapping("/{id}/parcours")
    public List<ParcoursSummaryResponse> parcoursDeLaZone(@PathVariable Long id) {
        return zoneService.parcoursDeLaZone(id);
    }

    @Operation(summary = "Creer une zone",
               description = """
                       Le polygone est fourni au format GeoJSON. Apres creation,
                       tous les parcours sont automatiquement rattaches a la plus
                       petite zone qui les contient.
                       """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Zone creee"),
            @ApiResponse(responseCode = "400", description = "Donnees invalides"),
            @ApiResponse(responseCode = "409", description = "Polygone invalide"),
            @ApiResponse(responseCode = "403", description = "Reserve aux administrateurs")
    })
    @PostMapping
    public ResponseEntity<ZoneResponse> creer(@Valid @RequestBody ZoneRequest requete,
                                              UriComponentsBuilder uriBuilder) {
        ZoneResponse creee = zoneService.creer(requete);
        URI localisation = uriBuilder.path("/api/zones/{id}")
                                     .buildAndExpand(creee.id()).toUri();
        return ResponseEntity.created(localisation).body(creee);
    }

    @Operation(summary = "Modifier une zone",
               description = "Les rattachements sont recalcules apres modification "
                           + "du contour.")
    @PutMapping("/{id}")
    public ZoneResponse modifier(@PathVariable Long id,
                                 @Valid @RequestBody ZoneRequest requete) {
        return zoneService.modifier(id, requete);
    }

    @Operation(summary = "Supprimer une zone",
               description = "Les parcours qui y etaient rattaches sont detaches, "
                           + "mais ne sont pas supprimes.")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    public void supprimer(@PathVariable Long id) {
        zoneService.supprimer(id);
    }

    @Operation(summary = "Relancer le rattachement automatique",
               description = """
                       Recalcule le rattachement de TOUS les parcours ayant un
                       trace, via ST_Within. Utile apres un import massif ou une
                       correction de contours.
                       """)
    @PostMapping("/rattachement")
    public Map<String, Object> rattacherTous() {
        int nb = zoneService.rattacherTousLesParcours();
        return Map.of("parcoursTraites", nb);
    }
}
