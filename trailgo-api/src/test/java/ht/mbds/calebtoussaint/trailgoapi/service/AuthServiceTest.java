// src/test/java/ht/mbds/calebtoussaint/trailgoapi/service/AuthServiceTest.java
package ht.mbds.calebtoussaint.trailgoapi.service;

import ht.mbds.calebtoussaint.trailgoapi.domain.Utilisateur;
import ht.mbds.calebtoussaint.trailgoapi.domain.enums.Role;
import ht.mbds.calebtoussaint.trailgoapi.exception.AuthentificationException;
import ht.mbds.calebtoussaint.trailgoapi.exception.RegleMetierException;
import ht.mbds.calebtoussaint.trailgoapi.exception.RessourceIntrouvableException;
import ht.mbds.calebtoussaint.trailgoapi.repository.UtilisateurRepository;
import ht.mbds.calebtoussaint.trailgoapi.security.JwtService;
import ht.mbds.calebtoussaint.trailgoapi.web.dto.AuthResponse;
import ht.mbds.calebtoussaint.trailgoapi.web.dto.ConnexionRequest;
import ht.mbds.calebtoussaint.trailgoapi.web.dto.InscriptionRequest;
import ht.mbds.calebtoussaint.trailgoapi.web.dto.UtilisateurResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires d'AuthService.
 *
 * Points sensibles verifies ici :
 *   - le role TOURISTE est impose a l'inscription, jamais choisi
 *     par le client
 *   - le mot de passe est hache et jamais renvoye
 *   - le message d'erreur de connexion est identique que l'email soit
 *     inconnu ou le mot de passe faux
 *
 * La duree de validite du jeton est injectee par @Value ; en test
 * unitaire il n'y a pas de contexte Spring, on la pose donc a la main
 * avec ReflectionTestUtils.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;

    @InjectMocks private AuthService service;

    private static final String EMAIL = "touriste@trailgo.ht";
    private static final String MOT_DE_PASSE = "MotDePasse123";
    private static final String HASH = "$2a$10$hashfictifpourletest";
    private static final String JETON = "eyJhbGciOiJIUzI1NiJ9.charge.signature";

    @BeforeEach
    void preparer() {
        ReflectionTestUtils.setField(service, "dureeValiditeMs", 86_400_000L);
    }

    private Utilisateur utilisateur(Role role, boolean actif) {
        Utilisateur u = new Utilisateur();
        u.setId(2L);
        u.setEmail(EMAIL);
        u.setMotDePasse(HASH);
        u.setNom("Caleb Toussaint");
        u.setRole(role);
        u.setActif(actif);
        return u;
    }

    // =================================================================
    @Nested
    @DisplayName("inscription")
    class Inscription {

        @Test
        @DisplayName("cree le compte avec le role TOURISTE")
        void creeAvecRoleTouriste() {
            when(utilisateurRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
            when(passwordEncoder.encode(MOT_DE_PASSE)).thenReturn(HASH);
            when(utilisateurRepository.save(any(Utilisateur.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(jwtService.genererJeton(any(Utilisateur.class))).thenReturn(JETON);

            AuthResponse resultat = service.inscrire(
                    new InscriptionRequest(EMAIL, MOT_DE_PASSE, "Caleb Toussaint"));

            // Le role n'est jamais choisi par le client : sinon
            // n'importe qui pourrait s'inscrire comme administrateur.
            assertThat(resultat.role()).isEqualTo(Role.TOURISTE);
            assertThat(resultat.jeton()).isEqualTo(JETON);
            assertThat(resultat.typeJeton()).isEqualTo("Bearer");
        }

        @Test
        @DisplayName("hache le mot de passe avant enregistrement")
        void hacheLeMotDePasse() {
            when(utilisateurRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
            when(passwordEncoder.encode(MOT_DE_PASSE)).thenReturn(HASH);
            when(utilisateurRepository.save(any(Utilisateur.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(jwtService.genererJeton(any(Utilisateur.class))).thenReturn(JETON);

            service.inscrire(new InscriptionRequest(EMAIL, MOT_DE_PASSE, "Caleb"));

            ArgumentCaptor<Utilisateur> capture = ArgumentCaptor.forClass(Utilisateur.class);
            verify(utilisateurRepository).save(capture.capture());

            // La base ne doit jamais contenir le mot de passe en clair.
            assertThat(capture.getValue().getMotDePasse()).isEqualTo(HASH);
            assertThat(capture.getValue().getMotDePasse()).isNotEqualTo(MOT_DE_PASSE);
        }

        @Test
        @DisplayName("normalise l'email en minuscules")
        void normaliseLEmail() {
            when(utilisateurRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn(HASH);
            when(utilisateurRepository.save(any(Utilisateur.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(jwtService.genererJeton(any(Utilisateur.class))).thenReturn(JETON);

            service.inscrire(new InscriptionRequest(
                    "  TOURISTE@TrailGo.HT  ", MOT_DE_PASSE, "Caleb"));

            ArgumentCaptor<Utilisateur> capture = ArgumentCaptor.forClass(Utilisateur.class);
            verify(utilisateurRepository).save(capture.capture());
            assertThat(capture.getValue().getEmail()).isEqualTo("touriste@trailgo.ht");
        }

        @Test
        @DisplayName("refuse un email deja utilise")
        void refuseEmailExistant() {
            when(utilisateurRepository.existsByEmailIgnoreCase(anyString())).thenReturn(true);

            assertThatThrownBy(() -> service.inscrire(
                    new InscriptionRequest(EMAIL, MOT_DE_PASSE, "Caleb")))
                    .isInstanceOf(RegleMetierException.class)
                    .hasMessageContaining("existe deja");

            verify(utilisateurRepository, never()).save(any());
        }

        @Test
        @DisplayName("cree le compte actif")
        void creeLeCompteActif() {
            when(utilisateurRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn(HASH);
            when(utilisateurRepository.save(any(Utilisateur.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(jwtService.genererJeton(any(Utilisateur.class))).thenReturn(JETON);

            service.inscrire(new InscriptionRequest(EMAIL, MOT_DE_PASSE, "Caleb"));

            ArgumentCaptor<Utilisateur> capture = ArgumentCaptor.forClass(Utilisateur.class);
            verify(utilisateurRepository).save(capture.capture());
            assertThat(capture.getValue().isActif()).isTrue();
        }
    }

    // =================================================================
    @Nested
    @DisplayName("connexion")
    class Connexion {

        @Test
        @DisplayName("renvoie un jeton pour des identifiants valides")
        void connexionReussie() {
            Utilisateur utilisateur = utilisateur(Role.ADMIN, true);
            when(utilisateurRepository.findByEmailIgnoreCase(EMAIL))
                    .thenReturn(Optional.of(utilisateur));
            when(passwordEncoder.matches(MOT_DE_PASSE, HASH)).thenReturn(true);
            when(jwtService.genererJeton(utilisateur)).thenReturn(JETON);

            AuthResponse resultat = service.connecter(
                    new ConnexionRequest(EMAIL, MOT_DE_PASSE));

            assertThat(resultat.jeton()).isEqualTo(JETON);
            assertThat(resultat.role()).isEqualTo(Role.ADMIN);
            assertThat(resultat.expireDansSecondes()).isEqualTo(86_400);
        }

        @Test
        @DisplayName("refuse un email inconnu")
        void refuseEmailInconnu() {
            when(utilisateurRepository.findByEmailIgnoreCase(anyString()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.connecter(
                    new ConnexionRequest("inconnu@trailgo.ht", MOT_DE_PASSE)))
                    .isInstanceOf(AuthentificationException.class)
                    .hasMessage("Email ou mot de passe incorrect");
        }

        @Test
        @DisplayName("refuse un mot de passe errone avec le meme message")
        void refuseMotDePasseErrone() {
            when(utilisateurRepository.findByEmailIgnoreCase(EMAIL))
                    .thenReturn(Optional.of(utilisateur(Role.TOURISTE, true)));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

            // Message IDENTIQUE au cas de l'email inconnu : distinguer
            // les deux permettrait a un attaquant de decouvrir quels
            // emails sont inscrits.
            assertThatThrownBy(() -> service.connecter(
                    new ConnexionRequest(EMAIL, "MauvaisMotDePasse")))
                    .isInstanceOf(AuthentificationException.class)
                    .hasMessage("Email ou mot de passe incorrect");
        }

        @Test
        @DisplayName("refuse un compte desactive")
        void refuseCompteDesactive() {
            when(utilisateurRepository.findByEmailIgnoreCase(EMAIL))
                    .thenReturn(Optional.of(utilisateur(Role.TOURISTE, false)));
            when(passwordEncoder.matches(MOT_DE_PASSE, HASH)).thenReturn(true);

            assertThatThrownBy(() -> service.connecter(
                    new ConnexionRequest(EMAIL, MOT_DE_PASSE)))
                    .isInstanceOf(AuthentificationException.class)
                    .hasMessageContaining("desactive");
        }
    }

    // =================================================================
    @Nested
    @DisplayName("profil")
    class Profil {

        @Test
        @DisplayName("renvoie le profil de l'utilisateur connecte")
        void renvoieLeProfil() {
            when(utilisateurRepository.findByEmailIgnoreCase(EMAIL))
                    .thenReturn(Optional.of(utilisateur(Role.ADMIN, true)));

            UtilisateurResponse resultat = service.profil(EMAIL);

            assertThat(resultat.email()).isEqualTo(EMAIL);
            assertThat(resultat.nom()).isEqualTo("Caleb Toussaint");
            assertThat(resultat.role()).isEqualTo(Role.ADMIN);
            assertThat(resultat.actif()).isTrue();
        }

        @Test
        @DisplayName("leve une exception si l'utilisateur n'existe plus")
        void leveSiUtilisateurIntrouvable() {
            when(utilisateurRepository.findByEmailIgnoreCase(anyString()))
                    .thenReturn(Optional.empty());

            // Cas reel : un jeton encore valide dont le compte a ete
            // supprime entre-temps.
            assertThatThrownBy(() -> service.profil("supprime@trailgo.ht"))
                    .isInstanceOf(RessourceIntrouvableException.class);
        }
    }
}
