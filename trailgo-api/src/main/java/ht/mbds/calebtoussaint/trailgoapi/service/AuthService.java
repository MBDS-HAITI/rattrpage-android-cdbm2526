// src/main/java/ht/mbds/calebtoussaint/trailgoapi/service/AuthService.java
package ht.mbds.calebtoussaint.trailgoapi.service;

import ht.mbds.calebtoussaint.trailgoapi.domain.Utilisateur;
import ht.mbds.calebtoussaint.trailgoapi.domain.enums.Role;
import ht.mbds.calebtoussaint.trailgoapi.exception.AuthentificationException;
import ht.mbds.calebtoussaint.trailgoapi.exception.RegleMetierException;
import ht.mbds.calebtoussaint.trailgoapi.exception.RessourceIntrouvableException;
import ht.mbds.calebtoussaint.trailgoapi.repository.UtilisateurRepository;
import ht.mbds.calebtoussaint.trailgoapi.security.JwtService;
import ht.mbds.calebtoussaint.trailgoapi.web.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${trailgo.jwt.expiration-ms}")
    private long dureeValiditeMs;

    /**
     * Inscription. Le role TOURISTE est impose : on ne laisse jamais le
     * client choisir son role, sinon n'importe qui deviendrait admin.
     * La promotion en ADMIN se fait depuis le back office.
     */
    @Transactional
    public AuthResponse inscrire(InscriptionRequest requete) {
        if (utilisateurRepository.existsByEmailIgnoreCase(requete.email())) {
            throw new RegleMetierException("Un compte existe deja avec cet email");
        }

        Utilisateur utilisateur = Utilisateur.builder()
                .email(requete.email().toLowerCase().trim())
                // Le mot de passe est hache : la base ne contient jamais le clair.
                .motDePasse(passwordEncoder.encode(requete.motDePasse()))
                .nom(requete.nom().trim())
                .role(Role.TOURISTE)
                .actif(true)
                .build();

        return construireReponse(utilisateurRepository.save(utilisateur));
    }

    /**
     * Connexion.
     *
     * Le message d'erreur est VOLONTAIREMENT identique que l'email soit
     * inconnu ou le mot de passe faux. Distinguer les deux permettrait a
     * un attaquant de decouvrir quels emails sont inscrits.
     */
    public AuthResponse connecter(ConnexionRequest requete) {
        Utilisateur utilisateur = utilisateurRepository
                .findByEmailIgnoreCase(requete.email().trim())
                .orElseThrow(() -> new AuthentificationException("Email ou mot de passe incorrect"));

        if (!passwordEncoder.matches(requete.motDePasse(), utilisateur.getMotDePasse())) {
            throw new AuthentificationException("Email ou mot de passe incorrect");
        }
        if (!utilisateur.isActif()) {
            throw new AuthentificationException("Ce compte a ete desactive");
        }
        return construireReponse(utilisateur);
    }

    /** Profil de l'utilisateur connecte, a partir de l'email du jeton. */
    public UtilisateurResponse profil(String email) {
        Utilisateur utilisateur = utilisateurRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur", email));

        return new UtilisateurResponse(
                utilisateur.getId(), utilisateur.getEmail(), utilisateur.getNom(),
                utilisateur.getRole(), utilisateur.isActif(), utilisateur.getDateCreation());
    }

    private AuthResponse construireReponse(Utilisateur utilisateur) {
        return new AuthResponse(
                jwtService.genererJeton(utilisateur),
                "Bearer",
                utilisateur.getId(),
                utilisateur.getEmail(),
                utilisateur.getNom(),
                utilisateur.getRole(),
                dureeValiditeMs / 1000);
    }
}
