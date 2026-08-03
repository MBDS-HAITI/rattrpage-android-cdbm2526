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
 * Traduction du sujet : "Endpoints publics (consultation) vs proteges
 * (creation, modification)".
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                // CSRF sert a proteger les formulaires avec cookies de session.
                // Une API REST sans session n'en a pas besoin.
                .csrf(csrf -> csrf.disable())

                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // STATELESS : aucune session cote serveur. Toute l'information
                // d'authentification vient du jeton envoye a chaque requete.
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth

                        // --- Ouvert a tous ---
                        // Seules l'inscription et la connexion sont publiques.
                        // /api/auth/moi reste protege : sans cela l'endpoint
                        // recevrait un Authentication null et planterait en 500.
                        .requestMatchers("/api/auth/inscription",
                                "/api/auth/connexion").permitAll()

                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html",
                                "/v3/api-docs/**").permitAll()

                        // Les images doivent etre lisibles sans jeton, sinon
                        // ni React ni Android ne pourraient les afficher dans
                        // une balise <img>.
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                        // Recherches spatiales : publiques, l'application mobile doit
                        // pouvoir chercher "autour de moi" sans connexion.
                        .requestMatchers(HttpMethod.GET, "/api/recherche/**").permitAll()

                        // --- Consultation publique des parcours ---
                        .requestMatchers(HttpMethod.GET, "/api/parcours/**").permitAll()

                        // --- Administration ---
                        .requestMatchers(HttpMethod.POST,   "/api/parcours/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,    "/api/parcours/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/parcours/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST,   "/api/fichiers/**").hasRole("ADMIN")

                        // --- Tout le reste demande au moins une connexion ---
                        .anyRequest().authenticated())

                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * BCrypt : algorithme de hachage concu pour les mots de passe.
     * Volontairement lent, ce qui rend les attaques par force brute
     * impraticables. Il integre un "sel" aleatoire, donc deux utilisateurs
     * avec le meme mot de passe ont des empreintes differentes.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CORS : autorise le back office React (port 5173 en developpement)
     * a appeler l'API depuis un autre port. Sans cela, le navigateur
     * bloque les requetes.
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
