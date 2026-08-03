// src/main/java/ht/mbds/calebtoussaint/trailgoapi/web/controller/AuthController.java
package ht.mbds.calebtoussaint.trailgoapi.web.controller;

import ht.mbds.calebtoussaint.trailgoapi.service.AuthService;
import ht.mbds.calebtoussaint.trailgoapi.web.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Authentification",
     description = "Inscription, connexion et profil de l'utilisateur courant")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Creer un compte",
               description = "Le compte est cree avec le role TOURISTE. "
                           + "Renvoie directement un jeton, l'utilisateur "
                           + "n'a pas besoin de se reconnecter.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Compte cree"),
            @ApiResponse(responseCode = "400", description = "Donnees invalides"),
            @ApiResponse(responseCode = "409", description = "Email deja utilise")
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/inscription")
    public AuthResponse inscrire(@Valid @RequestBody InscriptionRequest requete) {
        return authService.inscrire(requete);
    }

    @Operation(summary = "Se connecter",
               description = "Renvoie un jeton JWT a placer dans l'en-tete "
                           + "Authorization: Bearer <jeton>")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Connexion reussie"),
            @ApiResponse(responseCode = "401", description = "Identifiants incorrects")
    })
    @PostMapping("/connexion")
    public AuthResponse connecter(@Valid @RequestBody ConnexionRequest requete) {
        return authService.connecter(requete);
    }

    /**
     * L'objet Authentication est rempli par le JwtAuthenticationFilter.
     * getName() renvoie l'email place dans le "subject" du jeton.
     */
    @Operation(summary = "Profil de l'utilisateur connecte",
               description = "Necessite un jeton valide.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profil renvoye"),
            @ApiResponse(responseCode = "403", description = "Jeton absent ou invalide")
    })
    @GetMapping("/moi")
    public UtilisateurResponse profil(Authentication authentication) {
        return authService.profil(authentication.getName());
    }
}
