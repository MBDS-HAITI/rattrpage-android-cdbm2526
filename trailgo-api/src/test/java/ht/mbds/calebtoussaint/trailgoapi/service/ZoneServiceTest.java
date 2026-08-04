// src/test/java/ht/mbds/calebtoussaint/trailgoapi/service/ZoneServiceTest.java
package ht.mbds.calebtoussaint.trailgoapi.service;

import ht.mbds.calebtoussaint.trailgoapi.domain.Parcours;
import ht.mbds.calebtoussaint.trailgoapi.domain.ZoneGeographique;
import ht.mbds.calebtoussaint.trailgoapi.domain.enums.Difficulte;
import ht.mbds.calebtoussaint.trailgoapi.domain.enums.StatutParcours;
import ht.mbds.calebtoussaint.trailgoapi.domain.enums.Theme;
import ht.mbds.calebtoussaint.trailgoapi.exception.RegleMetierException;
import ht.mbds.calebtoussaint.trailgoapi.exception.RessourceIntrouvableException;
import ht.mbds.calebtoussaint.trailgoapi.repository.ZoneGeographiqueRepository;
import ht.mbds.calebtoussaint.trailgoapi.util.GeoUtils;
import ht.mbds.calebtoussaint.trailgoapi.web.dto.GeoJsonPolygon;
import ht.mbds.calebtoussaint.trailgoapi.web.dto.ZoneRequest;
import ht.mbds.calebtoussaint.trailgoapi.web.dto.ZoneResponse;
import ht.mbds.calebtoussaint.trailgoapi.web.mapper.ParcoursMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ZoneService")
class ZoneServiceTest {

    @Mock private ZoneGeographiqueRepository zoneRepository;
    private ZoneService service;

    @BeforeEach
    void preparer() {
        service = new ZoneService(zoneRepository, new ParcoursMapper());
    }

    /** Rectangle ferme autour du centre de Port-au-Prince. */
    private List<List<Double>> anneauFerme() {
        return List.of(
                List.of(-72.36, 18.53),
                List.of(-72.32, 18.53),
                List.of(-72.32, 18.56),
                List.of(-72.36, 18.56),
                List.of(-72.36, 18.53));   // point de fermeture
    }

    /** Meme rectangle, sans le point de fermeture. */
    private List<List<Double>> anneauOuvert() {
        return List.of(
                List.of(-72.36, 18.53),
                List.of(-72.32, 18.53),
                List.of(-72.32, 18.56),
                List.of(-72.36, 18.56));
    }

    private ZoneRequest requete(List<List<Double>> anneau) {
        return new ZoneRequest("Centre-ville de Port-au-Prince", "Ouest",
                new GeoJsonPolygon("Polygon", List.of(anneau)));
    }

    private ZoneGeographique zone() {
        ZoneGeographique zone = new ZoneGeographique();
        zone.setId(1L);
        zone.setNom("Centre-ville de Port-au-Prince");
        zone.setRegionAdministrative("Ouest");
        zone.setPolygone(GeoUtils.factory().createPolygon(new Coordinate[]{
                new Coordinate(-72.36, 18.53),
                new Coordinate(-72.32, 18.53),
                new Coordinate(-72.32, 18.56),
                new Coordinate(-72.36, 18.56),
                new Coordinate(-72.36, 18.53)}));
        return zone;
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

    // =================================================================
    @Nested
    @DisplayName("creer")
    class Creer {

        @Test
        @DisplayName("enregistre une zone dont l'anneau est ferme")
        void creeAvecAnneauFerme() {
            when(zoneRepository.saveAndFlush(any(ZoneGeographique.class)))
                    .thenAnswer(invocation -> {
                        ZoneGeographique z = invocation.getArgument(0);
                        z.setId(1L);
                        return z;
                    });
            when(zoneRepository.calculerSuperficieKm2(1L)).thenReturn(14.1);
            when(zoneRepository.parcoursInclus(1L)).thenReturn(List.of());

            ZoneResponse resultat = service.creer(requete(anneauFerme()));

            assertThat(resultat.nom()).isEqualTo("Centre-ville de Port-au-Prince");
            assertThat(resultat.superficieKm2()).isEqualByComparingTo("14.100");
        }

        @Test
        @DisplayName("ferme automatiquement un anneau ouvert")
        void fermeAutomatiquementLAnneau() {
            when(zoneRepository.saveAndFlush(any(ZoneGeographique.class)))
                    .thenAnswer(invocation -> {
                        ZoneGeographique z = invocation.getArgument(0);
                        z.setId(1L);
                        return z;
                    });
            when(zoneRepository.calculerSuperficieKm2(1L)).thenReturn(14.1);
            when(zoneRepository.parcoursInclus(1L)).thenReturn(List.of());

            ZoneResponse resultat = service.creer(requete(anneauOuvert()));

            // 4 sommets fournis + le point de fermeture ajoute = 5.
            // Beaucoup d'outils de dessin cartographique omettent ce
            // dernier point ; le rejeter serait inutilement rigide.
            List<List<Double>> anneau = resultat.polygone().coordinates().get(0);
            assertThat(anneau).hasSize(5);
            assertThat(anneau.get(0)).isEqualTo(anneau.get(anneau.size() - 1));
        }

        @Test
        @DisplayName("relance le rattachement des parcours apres creation")
        void relanceLeRattachement() {
            when(zoneRepository.saveAndFlush(any(ZoneGeographique.class)))
                    .thenAnswer(invocation -> {
                        ZoneGeographique z = invocation.getArgument(0);
                        z.setId(1L);
                        return z;
                    });
            when(zoneRepository.calculerSuperficieKm2(1L)).thenReturn(14.1);
            when(zoneRepository.parcoursInclus(1L)).thenReturn(List.of());

            service.creer(requete(anneauFerme()));

            // Une nouvelle zone peut englober des parcours deja crees :
            // sans ce recalcul ils resteraient sans zone.
            verify(zoneRepository).rattacherTousLesParcours();
        }

        @Test
        @DisplayName("refuse un type autre que Polygon")
        void refuseUnAutreType() {
            ZoneRequest requete = new ZoneRequest("Zone", null,
                    new GeoJsonPolygon("LineString", List.of(anneauFerme())));

            assertThatThrownBy(() -> service.creer(requete))
                    .isInstanceOf(RegleMetierException.class)
                    .hasMessageContaining("Polygon");
        }

        @Test
        @DisplayName("refuse un anneau de moins de quatre positions")
        void refuseAnneauTropCourt() {
            ZoneRequest requete = requete(List.of(
                    List.of(-72.36, 18.53),
                    List.of(-72.32, 18.53)));

            assertThatThrownBy(() -> service.creer(requete))
                    .isInstanceOf(RegleMetierException.class)
                    .hasMessageContaining("4 positions");
        }

        @Test
        @DisplayName("refuse une latitude hors bornes")
        void refuseLatitudeHorsBornes() {
            ZoneRequest requete = requete(List.of(
                    List.of(-72.36, 200.0),
                    List.of(-72.32, 18.53),
                    List.of(-72.32, 18.56),
                    List.of(-72.36, 200.0)));

            assertThatThrownBy(() -> service.creer(requete))
                    .isInstanceOf(RegleMetierException.class)
                    .hasMessageContaining("Latitude");
        }
    }

    // =================================================================
    @Nested
    @DisplayName("lecture")
    class Lecture {

        @Test
        @DisplayName("renvoie la zone avec sa superficie et son nombre de parcours")
        void consulteLaZone() {
            when(zoneRepository.findById(1L)).thenReturn(Optional.of(zone()));
            when(zoneRepository.calculerSuperficieKm2(1L)).thenReturn(14.1);
            when(zoneRepository.parcoursInclus(1L)).thenReturn(List.of(parcours()));

            ZoneResponse resultat = service.consulter(1L);

            assertThat(resultat.nom()).isEqualTo("Centre-ville de Port-au-Prince");
            assertThat(resultat.nbParcours()).isEqualTo(1);
            assertThat(resultat.bbox()).containsExactly(-72.36, 18.53, -72.32, 18.56);
        }

        @Test
        @DisplayName("leve une exception si la zone n'existe pas")
        void leveSiZoneIntrouvable() {
            when(zoneRepository.findById(anyLong())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.consulter(99L))
                    .isInstanceOf(RessourceIntrouvableException.class);
        }

        @Test
        @DisplayName("renvoie les parcours contenus dans la zone")
        void listeLesParcoursDeLaZone() {
            when(zoneRepository.findById(1L)).thenReturn(Optional.of(zone()));
            when(zoneRepository.parcoursInclus(1L)).thenReturn(List.of(parcours()));

            assertThat(service.parcoursDeLaZone(1L)).hasSize(1);
        }

        @Test
        @DisplayName("liste toutes les zones")
        void listeToutesLesZones() {
            when(zoneRepository.findAll()).thenReturn(List.of(zone()));
            when(zoneRepository.calculerSuperficieKm2(1L)).thenReturn(14.1);
            when(zoneRepository.parcoursInclus(1L)).thenReturn(List.of());

            assertThat(service.lister()).hasSize(1);
        }
    }

    // =================================================================
    @Nested
    @DisplayName("modification et suppression")
    class ModificationEtSuppression {

        @Test
        @DisplayName("met a jour le nom et recalcule les rattachements")
        void modifieLaZone() {
            when(zoneRepository.findById(1L)).thenReturn(Optional.of(zone()));
            when(zoneRepository.calculerSuperficieKm2(1L)).thenReturn(14.1);
            when(zoneRepository.parcoursInclus(1L)).thenReturn(List.of());

            ZoneRequest requete = new ZoneRequest("Nouveau nom", "Ouest",
                    new GeoJsonPolygon("Polygon", List.of(anneauFerme())));

            assertThat(service.modifier(1L, requete).nom()).isEqualTo("Nouveau nom");

            // Le contour a change : les rattachements peuvent ne plus
            // etre valides.
            verify(zoneRepository).rattacherTousLesParcours();
        }

        @Test
        @DisplayName("supprime une zone existante")
        void supprimeLaZone() {
            ZoneGeographique zone = zone();
            when(zoneRepository.findById(1L)).thenReturn(Optional.of(zone));

            service.supprimer(1L);

            verify(zoneRepository).delete(zone);
        }

        @Test
        @DisplayName("leve une exception a la suppression d'une zone inexistante")
        void leveALaSuppressionSiIntrouvable() {
            when(zoneRepository.findById(anyLong())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.supprimer(99L))
                    .isInstanceOf(RessourceIntrouvableException.class);

            verify(zoneRepository, never()).delete(any());
        }

        @Test
        @DisplayName("relance le rattachement de tous les parcours")
        void relanceLeRattachementGlobal() {
            when(zoneRepository.rattacherTousLesParcours()).thenReturn(3);

            assertThat(service.rattacherTousLesParcours()).isEqualTo(3);
        }
    }
}
