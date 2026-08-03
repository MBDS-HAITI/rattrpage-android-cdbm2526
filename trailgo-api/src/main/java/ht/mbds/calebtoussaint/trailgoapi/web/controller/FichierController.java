// src/main/java/ht/mbds/calebtoussaint/trailgoapi/web/controller/FichierController.java
package ht.mbds.calebtoussaint.trailgoapi.web.controller;

import ht.mbds.calebtoussaint.trailgoapi.service.StockageFichierService;
import ht.mbds.calebtoussaint.trailgoapi.web.dto.FichierResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Tag(name = "Fichiers", description = "Upload des images de parcours et d'etapes")
@RestController
@RequestMapping("/api/fichiers")
@RequiredArgsConstructor
public class FichierController {

    private final StockageFichierService stockageService;

    /**
     * POST /api/fichiers/images
     *
     * consumes = MULTIPART_FORM_DATA : c'est ce qui fait apparaitre un
     * bouton "Choose file" dans Swagger au lieu d'un champ JSON.
     *
     * Reserve aux ADMIN par la regle SecurityConfig sur les POST.
     */
    @Operation(summary = "Televerser une image",
               description = """
                       Formats acceptes : jpg, jpeg, png, webp, gif. Taille max 10 Mo.
                       Renvoie une URL a placer dans le champ imageCouverture d'un
                       parcours ou photo d'une etape.
                       """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Image enregistree"),
            @ApiResponse(responseCode = "409", description = "Format ou fichier invalide"),
            @ApiResponse(responseCode = "403", description = "Reserve aux administrateurs")
    })
    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public FichierResponse televerser(@RequestParam("fichier") MultipartFile fichier) {

        String urlRelative = stockageService.enregistrerImage(fichier);

        String urlAbsolue = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(urlRelative)
                .toUriString();

        return new FichierResponse(
                urlRelative,
                urlAbsolue,
                fichier.getOriginalFilename(),
                fichier.getSize());
    }
}
