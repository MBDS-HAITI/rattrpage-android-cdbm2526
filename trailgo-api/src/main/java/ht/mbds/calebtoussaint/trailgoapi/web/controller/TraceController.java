// src/main/java/ht/mbds/calebtoussaint/trailgoapi/web/controller/TraceController.java
package ht.mbds.calebtoussaint.trailgoapi.web.controller;

import ht.mbds.calebtoussaint.trailgoapi.service.TraceService;
import ht.mbds.calebtoussaint.trailgoapi.web.dto.GeoJsonLineString;
import ht.mbds.calebtoussaint.trailgoapi.web.dto.TraceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Traces", description = "Import et export des traces cartographiques")
@RestController
@RequestMapping("/api/parcours/{parcoursId}/trace")
@RequiredArgsConstructor
public class TraceController {

    private final TraceService traceService;

    @Operation(summary = "Consulter le trace au format GeoJSON",
               description = """
                       Renvoie la geometrie, la distance calculee par PostGIS et
                       l'enveloppe geographique. Le champ "geometrie" est
                       directement exploitable par Leaflet cote React et par une
                       Polyline Google Maps cote Android.

                       Accessible sans authentification.
                       """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trace renvoye"),
            @ApiResponse(responseCode = "404", description = "Parcours inexistant ou sans trace")
    })
    @GetMapping
    public TraceResponse consulter(@PathVariable Long parcoursId) {
        return traceService.consulterTrace(parcoursId);
    }

    @Operation(summary = "Importer un trace GeoJSON",
               description = """
                       Remplace le trace existant. La distance totale est
                       recalculee automatiquement par PostGIS.

                       Rappel RFC 7946 : les coordonnees s'ecrivent
                       [longitude, latitude], dans cet ordre.
                       """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trace enregistre"),
            @ApiResponse(responseCode = "409", description = "Geometrie invalide"),
            @ApiResponse(responseCode = "403", description = "Reserve aux administrateurs")
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public TraceResponse importerGeoJson(@PathVariable Long parcoursId,
                                         @Valid @RequestBody GeoJsonLineString geoJson) {
        return traceService.importerGeoJson(parcoursId, geoJson);
    }

    @Operation(summary = "Importer un fichier GPX",
               description = """
                       Lit les points <trkpt> du fichier, ou a defaut les <rtept>.
                       La distance totale est recalculee automatiquement.
                       """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trace importe"),
            @ApiResponse(responseCode = "409", description = "Fichier GPX invalide"),
            @ApiResponse(responseCode = "403", description = "Reserve aux administrateurs")
    })
    @PostMapping(value = "/gpx", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TraceResponse importerGpx(@PathVariable Long parcoursId,
                                     @RequestParam("fichier") MultipartFile fichier) {
        return traceService.importerGpx(parcoursId, fichier);
    }

    @Operation(summary = "Supprimer le trace",
               description = "Le parcours ne pourra plus etre publie tant "
                           + "qu'un nouveau trace n'aura pas ete importe.")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping
    public void supprimer(@PathVariable Long parcoursId) {
        traceService.supprimerTrace(parcoursId);
    }
}
