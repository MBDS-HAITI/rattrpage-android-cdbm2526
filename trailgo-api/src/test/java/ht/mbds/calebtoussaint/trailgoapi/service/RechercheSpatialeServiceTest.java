// src/test/java/ht/mbds/calebtoussaint/trailgoapi/service/RechercheSpatialeServiceTest.java
package ht.mbds.calebtoussaint.trailgoapi.service;

import ht.mbds.calebtoussaint.trailgoapi.exception.RegleMetierException;
import ht.mbds.calebtoussaint.trailgoapi.repository.RechercheSpatialeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de RechercheSpatialeService.
 *
 * On ne teste pas PostGIS lui-meme (ce serait un test d'integration),
 * mais les verifications de bornes et les valeurs par defaut, qui
 * relevent bien de la logique metier.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RechercheSpatialeService")
class RechercheSpatialeServiceTest {

    @Mock private RechercheSpatialeRepository rechercheRepository;
    @InjectMocks private RechercheSpatialeService service;

    // =================================================================
    @Nested
    @DisplayName("verification des coordonnees")
    class Coordonnees {

        @ParameterizedTest
        @ValueSource(doubles = {-91, 91, 180})
        @DisplayName("refuse une latitude hors bornes")
        void refuseLatitudeHorsBornes(double latitude) {
            assertThatThrownBy(() -> service.parcoursProches(latitude, -72.33, 1000.0, null))
                    .isInstanceOf(RegleMetierException.class)
                    .hasMessageContaining("Latitude");
        }

        @ParameterizedTest
        @ValueSource(doubles = {-181, 181, 360})
        @DisplayName("refuse une longitude hors bornes")
        void refuseLongitudeHorsBornes(double longitude) {
            assertThatThrownBy(() -> service.parcoursProches(18.54, longitude, 1000.0, null))
                    .isInstanceOf(RegleMetierException.class)
                    .hasMessageContaining("Longitude");
        }

        @Test
        @DisplayName("refuse une position incomplete")
        void refusePositionIncomplete() {
            assertThatThrownBy(() -> service.parcoursProches(null, -72.33, 1000.0, null))
                    .isInstanceOf(RegleMetierException.class)
                    .hasMessageContaining("obligatoires");
        }

        @Test
        @DisplayName("accepte les bornes extremes valides")
        void accepteLesBornesExtremes() {
            when(rechercheRepository.parcoursDansRayon(
                    anyDouble(), anyDouble(), anyDouble(), anyInt()))
                    .thenReturn(List.of());

            assertThat(service.parcoursProches(90.0, 180.0, 1000.0, null)).isEmpty();
            assertThat(service.parcoursProches(-90.0, -180.0, 1000.0, null)).isEmpty();
        }
    }

    // =================================================================
    @Nested
    @DisplayName("verification du rayon")
    class Rayon {

        @Test
        @DisplayName("applique 5000 metres par defaut")
        void rayonParDefaut() {
            when(rechercheRepository.parcoursDansRayon(
                    anyDouble(), anyDouble(), anyDouble(), anyInt()))
                    .thenReturn(List.of());

            service.parcoursProches(18.54, -72.33, null, null);

            ArgumentCaptor<Double> rayon = ArgumentCaptor.forClass(Double.class);
            verify(rechercheRepository).parcoursDansRayon(
                    anyDouble(), anyDouble(), rayon.capture(), anyInt());
            assertThat(rayon.getValue()).isEqualTo(5000);
        }

        @ParameterizedTest
        @ValueSource(doubles = {0, -1, -1000})
        @DisplayName("refuse un rayon nul ou negatif")
        void refuseRayonNulOuNegatif(double rayon) {
            assertThatThrownBy(() -> service.parcoursProches(18.54, -72.33, rayon, null))
                    .isInstanceOf(RegleMetierException.class)
                    .hasMessageContaining("positif");
        }

        @Test
        @DisplayName("refuse un rayon superieur a 100 km")
        void refuseRayonTropGrand() {
            // Au-dela, la requete perd son sens et charge inutilement
            // la base : autant la rejeter explicitement.
            assertThatThrownBy(() -> service.parcoursProches(18.54, -72.33, 200_000.0, null))
                    .isInstanceOf(RegleMetierException.class)
                    .hasMessageContaining("trop grand");
        }
    }

    // =================================================================
    @Nested
    @DisplayName("limite de resultats")
    class Limite {

        @Test
        @DisplayName("applique 50 resultats par defaut")
        void limiteParDefaut() {
            when(rechercheRepository.etapesDansRayon(
                    anyDouble(), anyDouble(), anyDouble(), anyInt()))
                    .thenReturn(List.of());

            service.etapesProches(18.54, -72.33, 1000.0, null);

            ArgumentCaptor<Integer> limite = ArgumentCaptor.forClass(Integer.class);
            verify(rechercheRepository).etapesDansRayon(
                    anyDouble(), anyDouble(), anyDouble(), limite.capture());
            assertThat(limite.getValue()).isEqualTo(50);
        }

        @Test
        @DisplayName("plafonne la limite a 200")
        void plafonneLaLimite() {
            when(rechercheRepository.poisDansRayon(
                    anyDouble(), anyDouble(), anyDouble(), anyInt()))
                    .thenReturn(List.of());

            service.poisProches(18.54, -72.33, 1000.0, 99_999);

            ArgumentCaptor<Integer> limite = ArgumentCaptor.forClass(Integer.class);
            verify(rechercheRepository).poisDansRayon(
                    anyDouble(), anyDouble(), anyDouble(), limite.capture());
            assertThat(limite.getValue()).isEqualTo(200);
        }
    }

    // =================================================================
    @Nested
    @DisplayName("recherche par rectangle")
    class Rectangle {

        @Test
        @DisplayName("refuse des bornes incompletes")
        void refuseBornesIncompletes() {
            assertThatThrownBy(() ->
                    service.parcoursDansRectangle(-72.36, 18.53, null, 18.56, null))
                    .isInstanceOf(RegleMetierException.class)
                    .hasMessageContaining("obligatoires");
        }

        @Test
        @DisplayName("refuse un rectangle dont l'ouest depasse l'est")
        void refuseOuestSuperieurAEst() {
            assertThatThrownBy(() ->
                    service.parcoursDansRectangle(-72.30, 18.53, -72.36, 18.56, null))
                    .isInstanceOf(RegleMetierException.class)
                    .hasMessageContaining("ouest");
        }

        @Test
        @DisplayName("refuse un rectangle dont le sud depasse le nord")
        void refuseSudSuperieurANord() {
            assertThatThrownBy(() ->
                    service.parcoursDansRectangle(-72.36, 18.60, -72.32, 18.56, null))
                    .isInstanceOf(RegleMetierException.class)
                    .hasMessageContaining("sud");
        }

        @Test
        @DisplayName("accepte un rectangle coherent")
        void accepteRectangleCoherent() {
            when(rechercheRepository.parcoursDansRectangle(
                    anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyInt()))
                    .thenReturn(List.of());

            assertThat(service.parcoursDansRectangle(-72.36, 18.53, -72.32, 18.56, null))
                    .isEmpty();
        }
    }
}
