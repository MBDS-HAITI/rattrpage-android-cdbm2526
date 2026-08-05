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
 * L'ORDRE DES REGLES EST DETERMINANT : Spring Security applique la
 * PREMIERE regle qui correspond a l'URL. Les regles specifiques (avis)
 * sont placees avant les regles generiques (parcours) pour la meme
 * raison.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
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
                        .requestMatchers(HttpMethod.GET,  "/api/parcours/*/avis/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/parcours/*/avis").authenticated()
                        .requestMatchers("/api/avis/**").authenticated()

                        // ---------- Favoris ----------
                        .requestMatchers("/api/favoris/**").authenticated()

                        // ---------- Gestion des utilisateurs, reservee ADMIN ----------
                        .requestMatchers("/api/utilisateurs/**").hasRole("ADMIN")

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

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

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
