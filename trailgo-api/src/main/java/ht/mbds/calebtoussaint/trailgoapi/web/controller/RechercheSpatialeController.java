// src/main/java/ht/mbds/calebtoussaint/trailgoapi/web/controller/RechercheSpatialeController.java
package ht.mbds.calebtoussaint.trailgoapi.web.controller;

import ht.mbds.calebtoussaint.trailgoapi.service.RechercheSpatialeService;
import ht.mbds.calebtoussaint.trailgoapi.web.dto.EtapeProcheResponse;
import ht.mbds.calebtoussaint.trailgoapi.web.dto.ParcoursProcheResponse;
import ht.mbds.calebtoussaint.trailgoapi.web.dto.PoiProcheResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Recherches geographiques.
 *
 * Tous ces endpoints sont en lecture seule et publics : l'application
 * mobile doit pouvoir chercher "autour de moi" sans obliger le touriste
 * a se connecter.
 */
@Tag(name = "Recherche spatiale",
     description = "Recherches par proximite et par zone geographique (PostGIS)")
@RestController
@RequestMapping("/api/recherche")
@RequiredArgsConstructor
public class RechercheSpatialeController {

    private final RechercheSpatialeService rechercheService;

    @Operation(summary = "Parcours a proximite d'une position",
               description = """
                       Renvoie les parcours PUBLIES dont au moins une etape se
                       trouve dans le rayon indique, tries du plus proche au
                       plus lointain.

                       Implemente avec ST_DWithin sur des geographies, ce qui
                       permet d'exprimer le rayon en metres et d'exploiter
                       l'index GIST.

                       Exemple : /api/recherche/parcours-proches?lat=18.5450&lng=-72.3390&rayonM=2000
                       """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des parcours proches"),
            @ApiResponse(responseCode = "409", description = "Coordonnees ou rayon invalides")
    })
    @GetMapping("/parcours-proches")
    public List<ParcoursProcheResponse> parcoursProches(

            @Parameter(description = "Latitude WGS84", example = "18.5450", required = true)
            @RequestParam Double lat,

            @Parameter(description = "Longitude WGS84", example = "-72.3390", required = true)
            @RequestParam Double lng,

            @Parameter(description = "Rayon de recherche en metres (defaut 5000, max 100000)",
                       example = "2000")
            @RequestParam(required = false) Double rayonM,

            @Parameter(description = "Nombre maximal de resultats (defaut 50, max 200)")
            @RequestParam(required = false) Integer limite) {

        return rechercheService.parcoursProches(lat, lng, rayonM, limite);
    }

    @Operation(summary = "Parcours visibles dans un rectangle",
               description = """
                       Renvoie les parcours dont le trace traverse le rectangle
                       indique (ST_Intersects).

                       C'est l'endpoint que le back office React appelle quand
                       l'utilisateur deplace ou zoome la carte : Leaflet fournit
                       les quatre bornes via map.getBounds().

                       Exemple : /api/recherche/parcours-bbox?ouest=-72.36&sud=18.53&est=-72.32&nord=18.56
                       """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des parcours dans la zone"),
            @ApiResponse(responseCode = "409", description = "Bornes invalides ou incoherentes")
    })
    @GetMapping("/parcours-bbox")
    public List<ParcoursProcheResponse> parcoursDansRectangle(

            @Parameter(description = "Longitude minimale", example = "-72.36", required = true)
            @RequestParam Double ouest,

            @Parameter(description = "Latitude minimale", example = "18.53", required = true)
            @RequestParam Double sud,

            @Parameter(description = "Longitude maximale", example = "-72.32", required = true)
            @RequestParam Double est,

            @Parameter(description = "Latitude maximale", example = "18.56", required = true)
            @RequestParam Double nord,

            @RequestParam(required = false) Integer limite) {

        return rechercheService.parcoursDansRectangle(ouest, sud, est, nord, limite);
    }

    @Operation(summary = "Etapes a proximite d'une position",
               description = """
                       Alimente la detection d'arrivee a une etape dans
                       l'application Android : on interroge avec la position GPS
                       courante et un rayon de quelques dizaines de metres.

                       Exemple : /api/recherche/etapes-proches?lat=18.5479&lng=-72.3395&rayonM=100
                       """)
    @GetMapping("/etapes-proches")
    public List<EtapeProcheResponse> etapesProches(
            @RequestParam Double lat,
            @RequestParam Double lng,
            @Parameter(description = "Rayon en metres, defaut 5000")
            @RequestParam(required = false) Double rayonM,
            @RequestParam(required = false) Integer limite) {

        return rechercheService.etapesProches(lat, lng, rayonM, limite);
    }

    @Operation(summary = "Points d'interet a proximite",
               description = """
                       Alimente l'alerte "point d'interet a moins de 200 m" de
                       l'application Android.

                       Exemple : /api/recherche/poi-proches?lat=18.5450&lng=-72.3390&rayonM=200
                       """)
    @GetMapping("/poi-proches")
    public List<PoiProcheResponse> poisProches(
            @RequestParam Double lat,
            @RequestParam Double lng,
            @Parameter(description = "Rayon en metres, defaut 5000. "
                                   + "L'enonce prevoit 200 m cote mobile.")
            @RequestParam(required = false) Double rayonM,
            @RequestParam(required = false) Integer limite) {

        return rechercheService.poisProches(lat, lng, rayonM, limite);
    }
}
