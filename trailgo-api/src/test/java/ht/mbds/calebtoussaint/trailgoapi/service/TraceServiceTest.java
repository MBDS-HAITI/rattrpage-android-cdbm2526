// src/test/java/ht/mbds/calebtoussaint/trailgoapi/service/TraceServiceTest.java
package ht.mbds.calebtoussaint.trailgoapi.service;

import ht.mbds.calebtoussaint.trailgoapi.domain.Parcours;
import ht.mbds.calebtoussaint.trailgoapi.domain.enums.Difficulte;
import ht.mbds.calebtoussaint.trailgoapi.domain.enums.StatutParcours;
import ht.mbds.calebtoussaint.trailgoapi.domain.enums.Theme;
import ht.mbds.calebtoussaint.trailgoapi.exception.RegleMetierException;
import ht.mbds.calebtoussaint.trailgoapi.exception.RessourceIntrouvableException;
import ht.mbds.calebtoussaint.trailgoapi.repository.ParcoursRepository;
import ht.mbds.calebtoussaint.trailgoapi.util.GeoUtils;
import ht.mbds.calebtoussaint.trailgoapi.web.dto.GeoJsonLineString;
import ht.mbds.calebtoussaint.trailgoapi.web.dto.TraceResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de TraceService.
 *
 * Le calcul de distance est fait par PostGIS ; on le simule ici en
 * programmant la reponse du repository. Ce qui est reellement teste,
 * c'est la conversion GeoJSON et GPX vers une geometrie JTS, plus les
 * verifications de validite.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TraceService")
class TraceServiceTest {

    @Mock private ParcoursRepository parcoursRepository;
    @InjectMocks private TraceService service;

    private Parcours parcours() {
        Parcours p = new Parcours();
        p.setId(1L);
        p.setTitre("Le vieux Port-au-Prince historique");
        p.setTheme(Theme.HISTORIQUE);
        p.setDifficulte(Difficulte.FACILE);
        p.setStatut(StatutParcours.BROUILLON);
        return p;
    }

    private Parcours parcoursAvecTrace() {
        Parcours p = parcours();
        p.setTrace(GeoUtils.lineString(List.of(
                new Coordinate(-72.3395, 18.5479),
                new Coordinate(-72.3378, 18.5426))));
        return p;
    }

    private void simulerEnregistrement(Parcours parcours, double distanceKm) {
        when(parcoursRepository.findById(1L)).thenReturn(Optional.of(parcours));
        when(parcoursRepository.saveAndFlush(any(Parcours.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(parcoursRepository.calculerDistanceKm(1L)).thenReturn(distanceKm);
    }

    // =================================================================
    @Nested
    @DisplayName("import GeoJSON")
    class ImportGeoJson {

        @Test
        @DisplayName("enregistre un trace valide et sa distance")
        void enregistreUnTraceValide() {
            simulerEnregistrement(parcours(), 1.194);

            TraceResponse resultat = service.importerGeoJson(1L,
                    new GeoJsonLineString("LineString", List.of(
                            List.of(-72.3395, 18.5479),
                            List.of(-72.3378, 18.5426),
                            List.of(-72.3419, 18.5461))));

            assertThat(resultat.nbPoints()).isEqualTo(3);
            assertThat(resultat.distanceKm()).isEqualByComparingTo("1.194");
        }

        @Test
        @DisplayName("respecte l'ordre longitude latitude de la RFC 7946")
        void respecteLOrdreRfc7946() {
            simulerEnregistrement(parcours(), 1.0);

            TraceResponse resultat = service.importerGeoJson(1L,
                    new GeoJsonLineString("LineString", List.of(
                            List.of(-72.3395, 18.5479),
                            List.of(-72.3378, 18.5426))));

            // La sortie doit renvoyer exactement l'ordre d'entree :
            // [longitude, latitude]. Une inversion ici deplacerait tous
            // les parcours a l'autre bout du globe.
            List<Double> premierPoint = resultat.geometrie().coordinates().get(0);
            assertThat(premierPoint.get(0)).isEqualTo(-72.3395);   // longitude
            assertThat(premierPoint.get(1)).isEqualTo(18.5479);    // latitude
        }

        @Test
        @DisplayName("calcule l'enveloppe geographique")
        void calculeLaBbox() {
            simulerEnregistrement(parcours(), 1.0);

            TraceResponse resultat = service.importerGeoJson(1L,
                    new GeoJsonLineString("LineString", List.of(
                            List.of(-72.3419, 18.5426),
                            List.of(-72.3378, 18.5479))));

            // bbox = [ouest, sud, est, nord]
            assertThat(resultat.bbox()).containsExactly(
                    -72.3419, 18.5426, -72.3378, 18.5479);
        }

        @Test
        @DisplayName("refuse un type autre que LineString")
        void refuseUnAutreType() {
            assertThatThrownBy(() -> service.importerGeoJson(1L,
                    new GeoJsonLineString("Polygon", List.of(
                            List.of(-72.33, 18.54),
                            List.of(-72.34, 18.55)))))
                    .isInstanceOf(RegleMetierException.class)
                    .hasMessageContaining("LineString");
        }

        @Test
        @DisplayName("refuse un trace d'un seul point")
        void refuseUnSeulPoint() {
            assertThatThrownBy(() -> service.importerGeoJson(1L,
                    new GeoJsonLineString("LineString",
                            List.of(List.of(-72.33, 18.54)))))
                    .isInstanceOf(RegleMetierException.class)
                    .hasMessageContaining("2 points");
        }

        @Test
        @DisplayName("refuse une latitude hors bornes")
        void refuseLatitudeHorsBornes() {
            assertThatThrownBy(() -> service.importerGeoJson(1L,
                    new GeoJsonLineString("LineString", List.of(
                            List.of(-72.33, 200.0),
                            List.of(-72.34, 18.55)))))
                    .isInstanceOf(RegleMetierException.class)
                    .hasMessageContaining("Latitude");
        }

        @Test
        @DisplayName("refuse une coordonnee incomplete")
        void refuseCoordonneeIncomplete() {
            assertThatThrownBy(() -> service.importerGeoJson(1L,
                    new GeoJsonLineString("LineString", List.of(
                            List.of(-72.33),
                            List.of(-72.34, 18.55)))))
                    .isInstanceOf(RegleMetierException.class)
                    .hasMessageContaining("paire");
        }

        @Test
        @DisplayName("leve une exception si le parcours n'existe pas")
        void leveSiParcoursIntrouvable() {
            when(parcoursRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.importerGeoJson(99L,
                    new GeoJsonLineString("LineString", List.of(
                            List.of(-72.33, 18.54),
                            List.of(-72.34, 18.55)))))
                    .isInstanceOf(RessourceIntrouvableException.class);
        }
    }

    // =================================================================
    @Nested
    @DisplayName("import GPX")
    class ImportGpx {

        private static final String GPX_VALIDE = """
                <?xml version="1.0" encoding="UTF-8"?>
                <gpx version="1.1" creator="TrailGo">
                  <trk>
                    <name>Centre historique</name>
                    <trkseg>
                      <trkpt lat="18.5479" lon="-72.3395"><ele>25</ele></trkpt>
                      <trkpt lat="18.5426" lon="-72.3378"><ele>30</ele></trkpt>
                      <trkpt lat="18.5461" lon="-72.3419"><ele>28</ele></trkpt>
                    </trkseg>
                  </trk>
                </gpx>
                """;

        private MockMultipartFile fichier(String nom, String contenu) {
            return new MockMultipartFile("fichier", nom, "application/gpx+xml",
                    contenu.getBytes(StandardCharsets.UTF_8));
        }

        @Test
        @DisplayName("lit les points trkpt d'un fichier valide")
        void litLesPointsTrkpt() {
            simulerEnregistrement(parcours(), 1.194);

            TraceResponse resultat = service.importerGpx(1L,
                    fichier("parcours.gpx", GPX_VALIDE));

            assertThat(resultat.nbPoints()).isEqualTo(3);
            assertThat(resultat.geometrie().coordinates().get(0))
                    .containsExactly(-72.3395, 18.5479);
        }

        @Test
        @DisplayName("se rabat sur les points rtept en l'absence de trkpt")
        void litLesPointsRtept() {
            simulerEnregistrement(parcours(), 0.5);

            String gpxRoute = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <gpx version="1.1">
                      <rte>
                        <rtept lat="18.5479" lon="-72.3395"/>
                        <rtept lat="18.5426" lon="-72.3378"/>
                      </rte>
                    </gpx>
                    """;

            assertThat(service.importerGpx(1L, fichier("route.gpx", gpxRoute))
                    .nbPoints()).isEqualTo(2);
        }

        @Test
        @DisplayName("ignore les points aux coordonnees aberrantes")
        void ignoreLesPointsAberrants() {
            simulerEnregistrement(parcours(), 1.0);

            String gpxAvecAberration = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <gpx version="1.1">
                      <trk><trkseg>
                        <trkpt lat="18.5479" lon="-72.3395"/>
                        <trkpt lat="999" lon="-72.3378"/>
                        <trkpt lat="18.5426" lon="-72.3378"/>
                      </trkseg></trk>
                    </gpx>
                    """;

            // Le point aberrant est ecarte silencieusement plutot que de
            // faire echouer tout l'import : les traces GPS reels
            // contiennent frequemment quelques points parasites.
            assertThat(service.importerGpx(1L, fichier("bruit.gpx", gpxAvecAberration))
                    .nbPoints()).isEqualTo(2);
        }

        @Test
        @DisplayName("refuse un fichier vide")
        void refuseFichierVide() {
            MockMultipartFile vide = new MockMultipartFile(
                    "fichier", "vide.gpx", "application/gpx+xml", new byte[0]);

            assertThatThrownBy(() -> service.importerGpx(1L, vide))
                    .isInstanceOf(RegleMetierException.class)
                    .hasMessageContaining("vide");
        }

        @Test
        @DisplayName("refuse une extension autre que gpx")
        void refuseMauvaiseExtension() {
            assertThatThrownBy(() -> service.importerGpx(1L,
                    fichier("trace.txt", GPX_VALIDE)))
                    .isInstanceOf(RegleMetierException.class)
                    .hasMessageContaining(".gpx");
        }

        @Test
        @DisplayName("refuse un XML malforme")
        void refuseXmlMalforme() {
            assertThatThrownBy(() -> service.importerGpx(1L,
                    fichier("casse.gpx", "<gpx><trk>pas ferme")))
                    .isInstanceOf(RegleMetierException.class);
        }

        @Test
        @DisplayName("refuse un GPX comportant moins de deux points")
        void refuseTropPeuDePoints() {
            String gpxUnPoint = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <gpx><trk><trkseg>
                      <trkpt lat="18.5479" lon="-72.3395"/>
                    </trkseg></trk></gpx>
                    """;

            assertThatThrownBy(() -> service.importerGpx(1L,
                    fichier("court.gpx", gpxUnPoint)))
                    .isInstanceOf(RegleMetierException.class)
                    .hasMessageContaining("points");
        }
    }

    // =================================================================
    @Nested
    @DisplayName("consultation et suppression")
    class ConsultationEtSuppression {

        @Test
        @DisplayName("renvoie le trace enregistre")
        void renvoieLeTrace() {
            when(parcoursRepository.findById(1L))
                    .thenReturn(Optional.of(parcoursAvecTrace()));

            TraceResponse resultat = service.consulterTrace(1L);

            assertThat(resultat.parcoursId()).isEqualTo(1L);
            assertThat(resultat.nbPoints()).isEqualTo(2);
            assertThat(resultat.geometrie().type()).isEqualTo("LineString");
        }

        @Test
        @DisplayName("leve une exception si le parcours n'a pas de trace")
        void leveSiAucunTrace() {
            when(parcoursRepository.findById(1L)).thenReturn(Optional.of(parcours()));

            assertThatThrownBy(() -> service.consulterTrace(1L))
                    .isInstanceOf(RessourceIntrouvableException.class)
                    .hasMessageContaining("trace");
        }

        @Test
        @DisplayName("leve une exception si le parcours n'existe pas")
        void leveSiParcoursIntrouvable() {
            when(parcoursRepository.findById(anyLong())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.consulterTrace(99L))
                    .isInstanceOf(RessourceIntrouvableException.class);
        }

        @Test
        @DisplayName("la suppression efface trace, bbox et distance")
        void suppressionEffaceTout() {
            Parcours parcours = parcoursAvecTrace();
            when(parcoursRepository.findById(1L)).thenReturn(Optional.of(parcours));

            service.supprimerTrace(1L);

            assertThat(parcours.getTrace()).isNull();
            assertThat(parcours.getBbox()).isNull();
            assertThat(parcours.getDistanceTotaleKm()).isNull();
        }
    }
}
