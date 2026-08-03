// src/main/java/ht/mbds/calebtoussaint/trailgoapi/security/JwtAuthenticationFilter.java
package ht.mbds.calebtoussaint.trailgoapi.security;

import ht.mbds.calebtoussaint.trailgoapi.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtre execute AVANT chaque requete.
 *
 * Son role : lire l'en-tete "Authorization: Bearer <jeton>", verifier
 * le jeton, et si tout est correct, declarer l'utilisateur authentifie
 * aupres de Spring Security pour la duree de la requete.
 *
 * OncePerRequestFilter garantit une seule execution par requete, meme
 * en cas de redirection interne.
 *
 * IMPORTANT : ce filtre ne REJETTE jamais une requete. Si le jeton est
 * absent ou invalide, il laisse simplement passer sans authentifier.
 * C'est SecurityConfig qui decide ensuite si l'endpoint demandait une
 * authentification ou non.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String ENTETE = "Authorization";
    private static final String PREFIXE = "Bearer ";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest requete,
                                    @NonNull HttpServletResponse reponse,
                                    @NonNull FilterChain chaine)
            throws ServletException, IOException {

        String entete = requete.getHeader(ENTETE);

        if (entete != null && entete.startsWith(PREFIXE)) {
            String jeton = entete.substring(PREFIXE.length());
            try {
                Claims contenu = jwtService.lireContenu(jeton);
                String email = contenu.getSubject();
                String role = contenu.get("role", String.class);

                // Spring Security attend le prefixe "ROLE_" pour hasRole().
                var autorites = List.of(new SimpleGrantedAuthority("ROLE_" + role));

                var authentification = new UsernamePasswordAuthenticationToken(
                        email, null, autorites);

                SecurityContextHolder.getContext().setAuthentication(authentification);

            } catch (JwtException ex) {
                // Jeton expire, falsifie ou malforme : on n'authentifie pas.
                // Pas de log en niveau erreur : c'est un cas de fonctionnement
                // normal (jeton perime), pas une anomalie du serveur.
                SecurityContextHolder.clearContext();
            }
        }
        chaine.doFilter(requete, reponse);
    }
}
