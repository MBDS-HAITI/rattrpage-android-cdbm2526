// src/main/java/ht/mbds/calebtoussaint/trailgoapi/security/JwtService.java
package ht.mbds.calebtoussaint.trailgoapi.security;

import ht.mbds.calebtoussaint.trailgoapi.domain.Utilisateur;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Fabrication et verification des jetons JWT.
 *
 * Un JWT est compose de trois parties separees par des points :
 *   entete.charge_utile.signature
 *
 * L'entete et la charge utile sont juste du JSON encode en Base64 :
 * N'IMPORTE QUI PEUT LES LIRE. On n'y met donc jamais d'information
 * sensible, seulement l'identite et le role.
 *
 * C'est la signature qui protege : calculee avec une cle secrete connue
 * du seul serveur, elle rend toute modification detectable.
 */
@Service
public class JwtService {

    private final SecretKey cle;
    private final long dureeValiditeMs;

    public JwtService(@Value("${trailgo.jwt.secret}") String secretBase64,
                      @Value("${trailgo.jwt.expiration-ms}") long dureeValiditeMs) {
        // La cle doit faire au moins 256 bits pour l'algorithme HS256.
        this.cle = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretBase64));
        this.dureeValiditeMs = dureeValiditeMs;
    }

    /** Genere un jeton pour un utilisateur authentifie. */
    public String genererJeton(Utilisateur utilisateur) {
        Date maintenant = new Date();
        Date expiration = new Date(maintenant.getTime() + dureeValiditeMs);

        return Jwts.builder()
                .subject(utilisateur.getEmail())
                .claim("role", utilisateur.getRole().name())
                .claim("nom", utilisateur.getNom())
                .claim("id", utilisateur.getId())
                .issuedAt(maintenant)
                .expiration(expiration)
                .signWith(cle)
                .compact();
    }

    /**
     * Verifie la signature et l'expiration, puis renvoie le contenu.
     * Leve une JwtException si le jeton est invalide, expire ou falsifie.
     */
    public Claims lireContenu(String jeton) throws JwtException {
        return Jwts.parser()
                .verifyWith(cle)
                .build()
                .parseSignedClaims(jeton)
                .getPayload();
    }

    /** Email de l'utilisateur, stocke dans le champ "subject". */
    public String extraireEmail(String jeton) {
        return lireContenu(jeton).getSubject();
    }

    /** Role de l'utilisateur (ADMIN ou TOURISTE). */
    public String extraireRole(String jeton) {
        return lireContenu(jeton).get("role", String.class);
    }
}
