// src/main/java/ht/mbds/calebtoussaint/trailgoapi/config/OpenApiConfig.java
package ht.mbds.calebtoussaint.trailgoapi.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration de la documentation OpenAPI.
 *
 * Deux roles :
 *
 * 1. Donner un titre et une description a l'API. Sans cela Swagger
 *    affiche "OpenAPI definition", ce qui fait vide en soutenance.
 *
 * 2. Declarer le schema de securite Bearer. C'est ce qui fait apparaitre
 *    le bouton "Authorize" en haut de Swagger UI : une fois le jeton
 *    saisi, il est automatiquement ajoute a toutes les requetes sous la
 *    forme "Authorization: Bearer <jeton>".
 */
@Configuration
public class OpenApiConfig {

    private static final String SCHEMA_JWT = "bearerAuth";

    @Bean
    public OpenAPI trailGoOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TrailGo API")
                        .version("1.0.0")
                        .description("""
                                API de gestion de parcours touristiques.

                                Consommee par le back office React et l'application
                                Android. Toutes les geometries sont exprimees en
                                WGS84 (SRID 4326) ; les coordonnees GeoJSON suivent
                                l'ordre [longitude, latitude] impose par la RFC 7946.

                                Pour tester les endpoints proteges :
                                1. POST /api/auth/connexion pour obtenir un jeton
                                2. Cliquer sur "Authorize" et coller le jeton
                                """)
                        .contact(new Contact().name("Caleb Toussaint"))
                        .license(new License().name("Projet pedagogique MBDS")))
                .servers(List.of(new Server()
                        .url("http://localhost:8081")
                        .description("Environnement de developpement")))
                .components(new Components().addSecuritySchemes(SCHEMA_JWT,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Jeton obtenu via POST /api/auth/connexion. "
                                           + "Coller uniquement le jeton, sans prefixe.")))
                // Applique le schema a tous les endpoints par defaut.
                .addSecurityItem(new SecurityRequirement().addList(SCHEMA_JWT));
    }
}
