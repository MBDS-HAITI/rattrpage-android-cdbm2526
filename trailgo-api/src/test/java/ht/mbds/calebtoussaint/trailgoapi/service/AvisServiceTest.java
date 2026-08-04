// src/test/java/ht/mbds/calebtoussaint/trailgoapi/service/AvisServiceTest.java
package ht.mbds.calebtoussaint.trailgoapi.service;

import ht.mbds.calebtoussaint.trailgoapi.domain.Avis;
import ht.mbds.calebtoussaint.trailgoapi.domain.Parcours;
import ht.mbds.calebtoussaint.trailgoapi.domain.Utilisateur;
import ht.mbds.calebtoussaint.trailgoapi.domain.enums.Difficulte;
import ht.mbds.calebtoussaint.trailgoapi.domain.enums.Role;
import ht.mbds.calebtoussaint.trailgoapi.domain.enums.StatutParcours;
import ht.mbds.calebtoussaint.trailgoapi.domain.enums.Theme;
import ht.mbds.calebtoussaint.trailgoapi.exception.RegleMetierException;
import ht.mbds.calebtoussaint.trailgoapi.exception.RessourceIntrouvableException;
import ht.mbds.calebtoussaint.trailgoapi.repository.AvisRepository;
import ht.mbds.calebtoussaint.trailgoapi.repository.ParcoursRepository;
import ht.mbds.calebtoussaint.trailgoapi.repository.UtilisateurRepository;
import ht.mbds.calebtoussaint.trailgoapi.web.dto.AvisRequest;
import ht.mbds.calebtoussaint.trailgoapi.web.dto.AvisResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires d'AvisService.
 *
 * Couvre les cinq regles metier :
 *   - un seul avis par utilisateur et par parcours
 *   - notation reservee aux parcours publies
 *   - seul l'auteur modifie son avis
 *   - l'auteur ou un ADMIN peut supprimer
 *   - le signalement fait remonter l'avis en moderation
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AvisService")
class AvisServiceTest {

    @Mock private AvisRepository avisRepository;
    @Mock private ParcoursRepository parcoursRepository;
    @Mock private UtilisateurRepository utilisateurRepository;

    @InjectMocks private AvisService service;

    private static final String EMAIL_AUTEUR = "touriste@trailgo.ht";
    private static final String EMAIL_AUTRE  = "autre@trailgo.ht";

    // =================================================================
    // Jeux de donnees
    // =================================================================

    private Utilisateur utilisateur(Long id, String email, Role role) {
        Utilisateur u = new Utilisateur();
        u.setId(id);
        u.setEmail(email);
        u.setNom("Utilisateur " + id);
        u.setRole(role);
        u.setActif(true);
        return u;
    }

    private Parcours parcours(StatutParcours statut) {
        Parcours p = new Parcours();
        p.setId(1L);
        p.setTitre("Le vieux Port-au-Prince historique");
        p.setTheme(Theme.HISTORIQUE);
        p.setDifficulte(Difficulte.FACILE);
        p.setStatut(statut);
        return p;
    }

    private Avis avisDe(Utilisateur auteur) {
        Avis avis = new Avis();
        avis.setId(5L);
        avis.setParcours(parcours(StatutParcours.PUBLIE));
        avis.setAuteur(auteur);
        avis.setNote((short) 4);
        avis.setCommentaire("Tres bien");
        avis.setSignale(false);
        return avis;
    }

    // =================================================================
    @Nested
    @DisplayName("deposer")
    class Deposer {

        @Test
        @DisplayName("enregistre un avis sur un parcours publie")
        void enregistreSurParcoursPublie() {
            Utilisateur auteur = utilisateur(2L, EMAIL_AUTEUR, Role.TOURISTE);

            when(parcoursRepository.findById(1L))
                    .thenReturn(Optional.of(parcours(StatutParcours.PUBLIE)));
            when(utilisateurRepository.findByEmailIgnoreCase(EMAIL_AUTEUR))
                    .thenReturn(Optional.of(auteur));
            when(avisRepository.existsByParcoursIdAndAuteurId(1L, 2L)).thenReturn(false);
            when(avisRepository.save(any(Avis.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            AvisResponse resultat = service.deposer(
                    1L, new AvisRequest((short) 5, "Excellent"), EMAIL_AUTEUR);

            assertThat(resultat.note()).isEqualTo((short) 5);
            assertThat(resultat.commentaire()).isEqualTo("Excellent");
            assertThat(resultat.auteurNom()).isEqualTo("Utilisateur 2");
            assertThat(resultat.signale()).isFalse();
        }

        @Test
        @DisplayName("refuse un avis sur un parcours en brouillon")
        void refuseSurBrouillon() {
            when(parcoursRepository.findById(1L))
                    .thenReturn(Optional.of(parcours(StatutParcours.BROUILLON)));

            assertThatThrownBy(() -> service.deposer(
                    1L, new AvisRequest((short) 5, null), EMAIL_AUTEUR))
                    .isInstanceOf(RegleMetierException.class)
                    .hasMessageContaining("publies");

            verify(avisRepository, never()).save(any());
        }

        @Test
        @DisplayName("refuse un second avis du meme utilisateur")
        void refuseLeDoublon() {
            when(parcoursRepository.findById(1L))
                    .thenReturn(Optional.of(parcours(StatutParcours.PUBLIE)));
            when(utilisateurRepository.findByEmailIgnoreCase(EMAIL_AUTEUR))
                    .thenReturn(Optional.of(utilisateur(2L, EMAIL_AUTEUR, Role.TOURISTE)));
            when(avisRepository.existsByParcoursIdAndAuteurId(1L, 2L)).thenReturn(true);

            // La base porte deja une contrainte d'unicite ; on la
            // verifie ici pour renvoyer un message clair plutot qu'une
            // erreur SQL brute.
            assertThatThrownBy(() -> service.deposer(
                    1L, new AvisRequest((short) 3, null), EMAIL_AUTEUR))
                    .isInstanceOf(RegleMetierException.class)
                    .hasMessageContaining("deja");
        }

        @Test
        @DisplayName("leve une exception si le parcours n'existe pas")
        void leveSiParcoursIntrouvable() {
            when(parcoursRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deposer(
                    99L, new AvisRequest((short) 4, null), EMAIL_AUTEUR))
                    .isInstanceOf(RessourceIntrouvableException.class);
        }
    }

    // =================================================================
    @Nested
    @DisplayName("modifier")
    class Modifier {

        @Test
        @DisplayName("l'auteur peut modifier son avis")
        void auteurPeutModifier() {
            Utilisateur auteur = utilisateur(2L, EMAIL_AUTEUR, Role.TOURISTE);
            when(avisRepository.findById(5L)).thenReturn(Optional.of(avisDe(auteur)));
            when(utilisateurRepository.findByEmailIgnoreCase(EMAIL_AUTEUR))
                    .thenReturn(Optional.of(auteur));

            AvisResponse resultat = service.modifier(
                    5L, new AvisRequest((short) 2, "Finalement moins bien"), EMAIL_AUTEUR);

            assertThat(resultat.note()).isEqualTo((short) 2);
            assertThat(resultat.commentaire()).isEqualTo("Finalement moins bien");
        }

        @Test
        @DisplayName("un autre utilisateur ne peut pas modifier")
        void autreUtilisateurRefuse() {
            when(avisRepository.findById(5L))
                    .thenReturn(Optional.of(avisDe(utilisateur(2L, EMAIL_AUTEUR, Role.TOURISTE))));
            when(utilisateurRepository.findByEmailIgnoreCase(EMAIL_AUTRE))
                    .thenReturn(Optional.of(utilisateur(3L, EMAIL_AUTRE, Role.TOURISTE)));

            assertThatThrownBy(() -> service.modifier(
                    5L, new AvisRequest((short) 1, "Sabotage"), EMAIL_AUTRE))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("meme un ADMIN ne peut pas reecrire l'avis d'autrui")
        void adminNePeutPasReecrire() {
            when(avisRepository.findById(5L))
                    .thenReturn(Optional.of(avisDe(utilisateur(2L, EMAIL_AUTEUR, Role.TOURISTE))));
            when(utilisateurRepository.findByEmailIgnoreCase(EMAIL_AUTRE))
                    .thenReturn(Optional.of(utilisateur(1L, EMAIL_AUTRE, Role.ADMIN)));

            // Un ADMIN peut SUPPRIMER un avis inapproprie, mais modifier
            // les mots d'autrui serait une falsification.
            assertThatThrownBy(() -> service.modifier(
                    5L, new AvisRequest((short) 5, "Modifie par l'admin"), EMAIL_AUTRE))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    // =================================================================
    @Nested
    @DisplayName("supprimer")
    class Supprimer {

        @Test
        @DisplayName("l'auteur peut supprimer son avis")
        void auteurPeutSupprimer() {
            Utilisateur auteur = utilisateur(2L, EMAIL_AUTEUR, Role.TOURISTE);
            Avis avis = avisDe(auteur);
            when(avisRepository.findById(5L)).thenReturn(Optional.of(avis));
            when(utilisateurRepository.findByEmailIgnoreCase(EMAIL_AUTEUR))
                    .thenReturn(Optional.of(auteur));

            service.supprimer(5L, EMAIL_AUTEUR);

            verify(avisRepository).delete(avis);
        }

        @Test
        @DisplayName("un ADMIN peut supprimer l'avis d'un autre")
        void adminPeutSupprimer() {
            Avis avis = avisDe(utilisateur(2L, EMAIL_AUTEUR, Role.TOURISTE));
            when(avisRepository.findById(5L)).thenReturn(Optional.of(avis));
            when(utilisateurRepository.findByEmailIgnoreCase(EMAIL_AUTRE))
                    .thenReturn(Optional.of(utilisateur(1L, EMAIL_AUTRE, Role.ADMIN)));

            service.supprimer(5L, EMAIL_AUTRE);

            verify(avisRepository).delete(avis);
        }

        @Test
        @DisplayName("un touriste tiers ne peut pas supprimer")
        void tiersRefuse() {
            when(avisRepository.findById(5L))
                    .thenReturn(Optional.of(avisDe(utilisateur(2L, EMAIL_AUTEUR, Role.TOURISTE))));
            when(utilisateurRepository.findByEmailIgnoreCase(EMAIL_AUTRE))
                    .thenReturn(Optional.of(utilisateur(3L, EMAIL_AUTRE, Role.TOURISTE)));

            assertThatThrownBy(() -> service.supprimer(5L, EMAIL_AUTRE))
                    .isInstanceOf(AccessDeniedException.class);

            verify(avisRepository, never()).delete(any());
        }
    }

    // =================================================================
    @Nested
    @DisplayName("signalement")
    class Signalement {

        @Test
        @DisplayName("le signalement marque l'avis")
        void signaleMarqueLAvis() {
            Avis avis = avisDe(utilisateur(2L, EMAIL_AUTEUR, Role.TOURISTE));
            when(avisRepository.findById(5L)).thenReturn(Optional.of(avis));

            assertThat(service.signaler(5L).signale()).isTrue();
        }

        @Test
        @DisplayName("la levee du signalement remet l'avis en ligne")
        void leveeDuSignalement() {
            Avis avis = avisDe(utilisateur(2L, EMAIL_AUTEUR, Role.TOURISTE));
            avis.setSignale(true);
            when(avisRepository.findById(5L)).thenReturn(Optional.of(avis));

            assertThat(service.leverSignalement(5L).signale()).isFalse();
        }

        @Test
        @DisplayName("leve une exception si l'avis n'existe pas")
        void leveSiAvisIntrouvable() {
            when(avisRepository.findById(anyLong())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.signaler(99L))
                    .isInstanceOf(RessourceIntrouvableException.class);
        }
    }
}
