// src/test/java/ht/mbds/calebtoussaint/trailgoapi/service/FavoriServiceTest.java
package ht.mbds.calebtoussaint.trailgoapi.service;

import ht.mbds.calebtoussaint.trailgoapi.domain.Favori;
import ht.mbds.calebtoussaint.trailgoapi.domain.Parcours;
import ht.mbds.calebtoussaint.trailgoapi.domain.Utilisateur;
import ht.mbds.calebtoussaint.trailgoapi.domain.enums.Difficulte;
import ht.mbds.calebtoussaint.trailgoapi.domain.enums.Role;
import ht.mbds.calebtoussaint.trailgoapi.domain.enums.StatutParcours;
import ht.mbds.calebtoussaint.trailgoapi.domain.enums.Theme;
import ht.mbds.calebtoussaint.trailgoapi.exception.RegleMetierException;
import ht.mbds.calebtoussaint.trailgoapi.exception.RessourceIntrouvableException;
import ht.mbds.calebtoussaint.trailgoapi.repository.FavoriRepository;
import ht.mbds.calebtoussaint.trailgoapi.repository.ParcoursRepository;
import ht.mbds.calebtoussaint.trailgoapi.repository.UtilisateurRepository;
import ht.mbds.calebtoussaint.trailgoapi.web.dto.FavoriResponse;
import ht.mbds.calebtoussaint.trailgoapi.web.mapper.ParcoursMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FavoriService")
class FavoriServiceTest {

    @Mock private FavoriRepository favoriRepository;
    @Mock private ParcoursRepository parcoursRepository;
    @Mock private UtilisateurRepository utilisateurRepository;

    private FavoriService service;

    private static final String EMAIL = "touriste@trailgo.ht";

    @BeforeEach
    void preparer() {
        service = new FavoriService(favoriRepository, parcoursRepository,
                                    utilisateurRepository, new ParcoursMapper());
    }

    private Utilisateur utilisateur() {
        Utilisateur u = new Utilisateur();
        u.setId(2L);
        u.setEmail(EMAIL);
        u.setNom("Touriste");
        u.setRole(Role.TOURISTE);
        return u;
    }

    private Parcours parcours() {
        Parcours p = new Parcours();
        p.setId(1L);
        p.setTitre("Le vieux Port-au-Prince historique");
        p.setTheme(Theme.HISTORIQUE);
        p.setDifficulte(Difficulte.FACILE);
        p.setStatut(StatutParcours.PUBLIE);
        return p;
    }

    @Test
    @DisplayName("ajoute un parcours aux favoris")
    void ajouteLeFavori() {
        when(utilisateurRepository.findByEmailIgnoreCase(EMAIL))
                .thenReturn(Optional.of(utilisateur()));
        when(parcoursRepository.findById(1L)).thenReturn(Optional.of(parcours()));
        when(favoriRepository.existsByUtilisateurIdAndParcoursId(2L, 1L)).thenReturn(false);
        when(favoriRepository.saveAndFlush(any(Favori.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FavoriResponse resultat = service.ajouter(1L, EMAIL);

        assertThat(resultat.parcours().id()).isEqualTo(1L);
        assertThat(resultat.parcours().titre())
                .isEqualTo("Le vieux Port-au-Prince historique");
    }

    @Test
    @DisplayName("refuse un favori en double")
    void refuseLeDoublon() {
        when(utilisateurRepository.findByEmailIgnoreCase(EMAIL))
                .thenReturn(Optional.of(utilisateur()));
        when(parcoursRepository.findById(1L)).thenReturn(Optional.of(parcours()));
        when(favoriRepository.existsByUtilisateurIdAndParcoursId(2L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.ajouter(1L, EMAIL))
                .isInstanceOf(RegleMetierException.class)
                .hasMessageContaining("deja");

        verify(favoriRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("leve une exception si le parcours n'existe pas")
    void leveSiParcoursIntrouvable() {
        when(utilisateurRepository.findByEmailIgnoreCase(EMAIL))
                .thenReturn(Optional.of(utilisateur()));
        when(parcoursRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.ajouter(99L, EMAIL))
                .isInstanceOf(RessourceIntrouvableException.class);
    }

    @Test
    @DisplayName("retire un favori existant")
    void retireLeFavori() {
        when(utilisateurRepository.findByEmailIgnoreCase(EMAIL))
                .thenReturn(Optional.of(utilisateur()));
        when(favoriRepository.existsByUtilisateurIdAndParcoursId(2L, 1L)).thenReturn(true);

        service.retirer(1L, EMAIL);

        verify(favoriRepository).deleteByUtilisateurIdAndParcoursId(2L, 1L);
    }

    @Test
    @DisplayName("leve une exception en retirant un favori absent")
    void leveSiFavoriAbsent() {
        when(utilisateurRepository.findByEmailIgnoreCase(EMAIL))
                .thenReturn(Optional.of(utilisateur()));
        when(favoriRepository.existsByUtilisateurIdAndParcoursId(2L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> service.retirer(1L, EMAIL))
                .isInstanceOf(RessourceIntrouvableException.class);
    }

    @Test
    @DisplayName("indique si un parcours est en favori")
    void indiqueLeStatut() {
        when(utilisateurRepository.findByEmailIgnoreCase(EMAIL))
                .thenReturn(Optional.of(utilisateur()));
        when(favoriRepository.existsByUtilisateurIdAndParcoursId(2L, 1L)).thenReturn(true);

        assertThat(service.estEnFavori(1L, EMAIL)).isTrue();
    }
}
