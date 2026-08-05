// src/main/java/ht/mbds/calebtoussaint/trailgoapi/web/controller/UtilisateurAdminController.java
package ht.mbds.calebtoussaint.trailgoapi.web.controller;

import ht.mbds.calebtoussaint.trailgoapi.service.UtilisateurAdminService;
import ht.mbds.calebtoussaint.trailgoapi.web.dto.ModifierUtilisateurRequest;
import ht.mbds.calebtoussaint.trailgoapi.web.dto.PageResponse;
import ht.mbds.calebtoussaint.trailgoapi.web.dto.UtilisateurAdminResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Utilisateurs", description = "Administration des comptes. Reserve aux administrateurs.")
@RestController
@RequestMapping("/api/utilisateurs")
@RequiredArgsConstructor
public class UtilisateurAdminController {

    private final UtilisateurAdminService utilisateurAdminService;

    @Operation(summary = "Lister les comptes utilisateurs")
    @GetMapping
    public PageResponse<UtilisateurAdminResponse> lister(
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return utilisateurAdminService.lister(pageable);
    }

    @Operation(summary = "Modifier le role et le statut d'un utilisateur",
               description = """
                       Un administrateur ne peut pas modifier son propre compte via
                       cet endpoint, et le dernier administrateur actif de la
                       plateforme ne peut etre ni retrograde ni desactive.
                       """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Utilisateur modifie"),
            @ApiResponse(responseCode = "404", description = "Utilisateur inexistant"),
            @ApiResponse(responseCode = "409", description = "Auto-modification ou "
                                                          + "dernier administrateur")
    })
    @PutMapping("/{id}")
    public UtilisateurAdminResponse modifier(@PathVariable Long id,
                                             @Valid @RequestBody ModifierUtilisateurRequest requete,
                                             Authentication authentication) {
        return utilisateurAdminService.modifier(id, requete, authentication.getName());
    }
}
