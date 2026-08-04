// src/main/java/ht/mbds/calebtoussaint/trailgoapi/security/SecurityConfig.java
package ht.mbds.calebtoussaint.trailgoapi.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Regles de securite de l'API.
 *
 * =====================================================================
 * L'ORDRE DES REGLES EST DETERMINANT
 * =====================================================================
 * Spring Security applique LA PREMIERE regle qui correspond a l'URL.
 *
 * Exemple concret : la regle
 *     POST /api/parcours/**  -> ADMIN
 * couvrirait aussi
 *     POST /api/parcours/1/avis
 * qui doit rester ouvert a tout utilisateur connecte.
 *
 * Les regles specifiques (avis) sont donc placees AVANT les regles
 * generiques (parcours). Inverser les deux empecherait tout touriste
 * de noter un parcours, avec un 403 difficile a diagnostiquer.
 * =====================================================================
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                // CSRF protege les formulaires avec cookies de session.
                // Une API REST sans session n'en a pas besoin.
                .csrf(csrf -> csrf.disable())

                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // STATELESS : aucune session cote serveur, toute
                // l'information d'authentification vient du jeton.
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth

                        // ---------- Ouvert a tous ----------
                        .requestMatchers("/api/auth/inscription",
                                "/api/auth/connexion").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html",
                                "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/recherche/**").permitAll()

                        // ---------- Moderation, avant les regles avis ----------
                        .requestMatchers(HttpMethod.GET,    "/api/avis/signales").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/avis/*/signalement").hasRole("ADMIN")

                        // ---------- Avis : lecture publique, ecriture connectee ----------
                        // CES REGLES DOIVENT PRECEDER CELLES DES PARCOURS.
                        .requestMatchers(HttpMethod.GET,  "/api/parcours/*/avis/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/parcours/*/avis").authenticated()
                        .requestMatchers("/api/avis/**").authenticated()

                        // ---------- Favoris : toujours authentifie ----------
                        .requestMatchers("/api/favoris/**").authenticated()

                        // ---------- Parcours ----------
                        .requestMatchers(HttpMethod.GET,    "/api/parcours/**").permitAll()
                        .requestMatchers(HttpMethod.POST,   "/api/parcours/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,    "/api/parcours/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/parcours/**").hasRole("ADMIN")

                        // ---------- Zones ----------
                        .requestMatchers(HttpMethod.GET,    "/api/zones/**").permitAll()
                        .requestMatchers(HttpMethod.POST,   "/api/zones/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,    "/api/zones/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/zones/**").hasRole("ADMIN")

                        // ---------- Fichiers ----------
                        .requestMatchers(HttpMethod.POST, "/api/fichiers/**").hasRole("ADMIN")

                        // ---------- Le reste demande une connexion ----------
                        .anyRequest().authenticated())

                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * BCrypt : algorithme concu pour les mots de passe. Volontairement
     * lent, ce qui rend la force brute impraticable, et integrant un sel
     * aleatoire, de sorte que deux mots de passe identiques donnent des
     * empreintes differentes.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CORS : autorise le back office React (port 5173 en developpement)
     * a appeler l'API depuis une autre origine.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://localhost:3000"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
