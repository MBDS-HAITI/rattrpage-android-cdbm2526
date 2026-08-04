// src/test/java/ht/mbds/calebtoussaint/trailgoapi/service/ParcoursServiceTest.java
package ht.mbds.calebtoussaint.trailgoapi.service;

import ht.mbds.calebtoussaint.trailgoapi.domain.Etape;
import ht.mbds.calebtoussaint.trailgoapi.domain.Parcours;
import ht.mbds.calebtoussaint.trailgoapi.domain.ZoneGeographique;
import ht.mbds.calebtoussaint.trailgoapi.domain.enums.Difficulte;
import ht.mbds.calebtoussaint.trailgoapi.domain.enums.StatutParcours;
import ht.mbds.calebtoussaint.trailgoapi.domain.enums.Theme;
import ht.mbds.calebtoussaint.trailgoapi.exception.RegleMetierException;
import ht.mbds.calebtoussaint.trailgoapi.exception.RessourceIntrouvableException;
import ht.mbds.calebtoussaint.trailgoapi.repository.ParcoursRepository;
import ht.mbds.calebtoussaint.trailgoapi.repository.ZoneGeographiqueRepository;
import ht.mbds.calebtoussaint.trailgoapi.util.GeoUtils;
import ht.mbds.calebtoussaint.trailgoapi.web.dto.*;
import ht.mbds.calebtoussaint.trailgoapi.web.mapper.ParcoursMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de ParcoursService.
 *
 * PRINCIPE DU TEST UNITAIRE : aucune base de donnees, aucun reseau.
 * Les repositories sont remplaces par des doublures Mockito (@Mock)
 * dont on programme les reponses. On teste ainsi la LOGIQUE METIER
 * isolement, en quelques millisecondes.
 *
 * @ExtendWith(MockitoExtension.class) : active Mockito sous JUnit 5.
 * @Mock        : cree une doublure.
 * @InjectMocks : instancie la classe testee en lui injectant les doublures.
 *
 * Le vrai ParcoursMapper est utilise (et non une doublure) : c'est une
 * classe sans dependance, la doubler n'apporterait rien et rendrait les
 * assertions moins parlantes.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ParcoursService")
class ParcoursServiceTest {

    @Mock
    private ParcoursRepository parcoursRepository;

    @Mock
    private ZoneGeographiqueRepository zoneRepository;

    private ParcoursMapper mapper;
    private ParcoursService service;

    @BeforeEach
    void preparer() {
        mapper = new ParcoursMapper();
        service = new ParcoursService(parcoursRepository, zoneRepository, mapper);
    }

    // =================================================================
    // Jeux de donnees
    // =================================================================

    private Parcours parcoursAvecEtapes() {
        Parcours parcours = new Parcours();
        parcours.setId(1L);
        parcours.setTitre("Le vieux Port-au-Prince historique");
        parcours.setTheme(Theme.HISTORIQUE);
        parcours.setDifficulte(Difficulte.FACILE);
        parcours.setStatut(StatutParcours.BROUILLON);
        parcours.setNbConsultations(0L);

        Etape etape = new Etape();
        etape.setId(10L);
        etape.setNom("Cathedrale Notre-Dame");
        etape.setPosition(GeoUtils.point(18.5479, -72.3395));
        etape.setOrdre(1);
        parcours.ajouterEtape(etape);

        return parcours;
    }

    private Parcours parcoursSansEtape() {
        Parcours parcours = new Parcours();
        parcours.setId(2L);
        parcours.setTitre("Parcours vide");
        parcours.setTheme(Theme.NATUREL);
        parcours.setDifficulte(Difficulte.MOYEN);
        parcours.setStatut(StatutParcours.BROUILLON);
        parcours.setNbConsultations(0L);
        return parcours;
    }

    private EtapeRequest etapeRequest(String nom, double lat, double lng) {
        return new EtapeRequest(nom, null, lat, lng, null, 30);
    }

    // =================================================================
    @Nested
    @DisplayName("consulter")
    class Consulter {

        @Test
        @DisplayName("renvoie le parcours et ses etapes")
        void renvoieLeParcours() {
            when(parcoursRepository.findByIdAvecEtapes(1L))
                    .thenReturn(Optional.of(parcoursAvecEtapes()));

            ParcoursResponse resultat = service.consulter(1L);

            assertThat(resultat.id()).isEqualTo(1L);
            assertThat(resultat.titre()).isEqualTo("Le vieux Port-au-Prince historique");
            assertThat(resultat.etapes()).hasSize(1);
        }

        @Test
        @DisplayName("convertit la geometrie en latitude et longitude")
        void convertitLaGeometrie() {
            when(parcoursRepository.findByIdAvecEtapes(1L))
                    .thenReturn(Optional.of(parcoursAvecEtapes()));

            EtapeResponse etape = service.consulter(1L).etapes().get(0);

            // Verifie qu'il n'y a pas d'inversion X/Y :
            // getY() = latitude, getX() = longitude.
            assertThat(etape.latitude()).isEqualTo(18.5479);
            assertThat(etape.longitude()).isEqualTo(-72.3395);
        }

        @Test
        @DisplayName("leve une exception si le parcours n'existe pas")
        void leveSiIntrouvable() {
            when(parcoursRepository.findByIdAvecEtapes(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.consulter(99L))
                    .isInstanceOf(RessourceIntrouvableException.class)
                    .hasMessageContaining("99");
        }
    }

    // =================================================================
    @Nested
    @DisplayName("rechercher")
    class Rechercher {

        @Test
        @DisplayName("remplace une recherche vide par le joker SQL")
        void rechercheVideDevientJoker() {
            Page<Parcours> page = new PageImpl<>(List.of(parcoursAvecEtapes()));
            when(parcoursRepository.rechercher(any(), any(), any(), any(), anyString(), any()))
                    .thenReturn(page);

            service.rechercher(null, null, null, null, null, PageRequest.of(0, 20));

            // Le motif doit valoir "%", jamais null : en SQL,
            // "titre LIKE null" n'est pas vrai mais inconnu, ce qui
            // exclurait toutes les lignes.
            ArgumentCaptor<String> motif = ArgumentCaptor.forClass(String.class);
            verify(parcoursRepository).rechercher(any(), any(), any(), any(),
                                                  motif.capture(), any());
            assertThat(motif.getValue()).isEqualTo("%");
        }

        @Test
        @DisplayName("encadre le terme recherche de jokers et le met en minuscules")
        void termeEncadreDeJokers() {
            when(parcoursRepository.rechercher(any(), any(), any(), any(), anyString(), any()))
                    .thenReturn(new PageImpl<>(List.of()));

            service.rechercher(null, null, null, null, "  PORT  ", PageRequest.of(0, 20));

            ArgumentCaptor<String> motif = ArgumentCaptor.forClass(String.class);
            verify(parcoursRepository).rechercher(any(), any(), any(), any(),
                                                  motif.capture(), any());
            assertThat(motif.getValue()).isEqualTo("%port%");
        }

        @Test
        @DisplayName("transmet les filtres au repository")
        void transmetLesFiltres() {
            when(parcoursRepository.rechercher(any(), any(), any(), any(), anyString(), any()))
                    .thenReturn(new PageImpl<>(List.of()));

            Pageable pageable = PageRequest.of(0, 10);
            service.rechercher(Theme.HISTORIQUE, Difficulte.FACILE,
                               StatutParcours.PUBLIE, 200, null, pageable);

            verify(parcoursRepository).rechercher(
                    eq(Theme.HISTORIQUE), eq(Difficulte.FACILE),
                    eq(StatutParcours.PUBLIE), eq(200), eq("%"), eq(pageable));
        }

        @Test
        @DisplayName("renvoie une enveloppe de pagination coherente")
        void enveloppeDePagination() {
            Page<Parcours> page = new PageImpl<>(
                    List.of(parcoursAvecEtapes()), PageRequest.of(0, 20), 1);
            when(parcoursRepository.rechercher(any(), any(), any(), any(), anyString(), any()))
                    .thenReturn(page);

            PageResponse<ParcoursSummaryResponse> resultat =
                    service.rechercher(null, null, null, null, null, PageRequest.of(0, 20));

            assertThat(resultat.contenu()).hasSize(1);
            assertThat(resultat.totalElements()).isEqualTo(1);
            assertThat(resultat.premiere()).isTrue();
            assertThat(resultat.derniere()).isTrue();
            assertThat(resultat.contenu().get(0).nbEtapes()).isEqualTo(1);
        }
    }

    // =================================================================
    @Nested
    @DisplayName("creer")
    class Creer {

        @Test
        @DisplayName("cree toujours le parcours en BROUILLON")
        void creeEnBrouillon() {
            when(parcoursRepository.save(any(Parcours.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            ParcoursRequest requete = new ParcoursRequest(
                    "Nouveau parcours", null, Theme.CULTUREL, Difficulte.FACILE,
                    120, null, null, List.of());

            ParcoursResponse resultat = service.creer(requete);

            // Meme si le client tentait d'imposer PUBLIE, le service
            // force BROUILLON : la publication passe par un endpoint
            // dedie qui verifie les prerequis.
            assertThat(resultat.statut()).isEqualTo(StatutParcours.BROUILLON);
        }

        @Test
        @DisplayName("renumerote les etapes de 1 a N")
        void renumeroteLesEtapes() {
            when(parcoursRepository.save(any(Parcours.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            ParcoursRequest requete = new ParcoursRequest(
                    "Parcours a trois etapes", null, Theme.CULTUREL, Difficulte.FACILE,
                    120, null, null,
                    List.of(etapeRequest("A", 18.54, -72.33),
                            etapeRequest("B", 18.55, -72.34),
                            etapeRequest("C", 18.56, -72.35)));

            ParcoursResponse resultat = service.creer(requete);

            // La renumerotation garantit qu'il n'y a ni trou ni doublon,
            // donc que la contrainte d'unicite (parcours_id, ordre)
            // ne peut pas etre violee.
            assertThat(resultat.etapes()).extracting(EtapeResponse::ordre)
                    .containsExactly(1, 2, 3);
            assertThat(resultat.etapes()).extracting(EtapeResponse::nom)
                    .containsExactly("A", "B", "C");
        }

        @Test
        @DisplayName("accepte un parcours sans etape")
        void accepteSansEtape() {
            when(parcoursRepository.save(any(Parcours.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            ParcoursRequest requete = new ParcoursRequest(
                    "Brouillon", null, Theme.NATUREL, Difficulte.MOYEN,
                    null, null, null, null);

            assertThat(service.creer(requete).etapes()).isEmpty();
        }

        @Test
        @DisplayName("leve une exception si la zone demandee n'existe pas")
        void leveSiZoneInconnue() {
            when(zoneRepository.findById(42L)).thenReturn(Optional.empty());

            ParcoursRequest requete = new ParcoursRequest(
                    "Parcours", null, Theme.CULTUREL, Difficulte.FACILE,
                    null, null, 42L, List.of());

            assertThatThrownBy(() -> service.creer(requete))
                    .isInstanceOf(RessourceIntrouvableException.class)
                    .hasMessageContaining("Zone");
        }

        @Test
        @DisplayName("rattache la zone quand elle existe")
        void rattacheLaZone() {
            ZoneGeographique zone = new ZoneGeographique();
            zone.setId(3L);
            zone.setNom("Centre-ville");
            when(zoneRepository.findById(3L)).thenReturn(Optional.of(zone));
            when(parcoursRepository.save(any(Parcours.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            ParcoursRequest requete = new ParcoursRequest(
                    "Parcours", null, Theme.CULTUREL, Difficulte.FACILE,
                    null, null, 3L, List.of());

            ParcoursResponse resultat = service.creer(requete);

            assertThat(resultat.zoneId()).isEqualTo(3L);
            assertThat(resultat.zoneNom()).isEqualTo("Centre-ville");
        }
    }

    // =================================================================
    @Nested
    @DisplayName("changerPublication")
    class ChangerPublication {

        @Test
        @DisplayName("publie un parcours qui a des etapes")
        void publieAvecEtapes() {
            when(parcoursRepository.findByIdAvecEtapes(1L))
                    .thenReturn(Optional.of(parcoursAvecEtapes()));

            ParcoursResponse resultat = service.changerPublication(1L, true);

            assertThat(resultat.statut()).isEqualTo(StatutParcours.PUBLIE);
        }

        @Test
        @DisplayName("refuse de publier un parcours sans etape")
        void refusePublicationSansEtape() {
            when(parcoursRepository.findByIdAvecEtapes(2L))
                    .thenReturn(Optional.of(parcoursSansEtape()));

            // Regle metier centrale : un touriste qui ouvrirait un
            // parcours publie mais vide verrait une carte blanche.
            assertThatThrownBy(() -> service.changerPublication(2L, true))
                    .isInstanceOf(RegleMetierException.class)
                    .hasMessageContaining("sans etape");
        }

        @Test
        @DisplayName("depublie sans exiger d'etape")
        void depublieSansContrainte() {
            Parcours parcours = parcoursSansEtape();
            parcours.setStatut(StatutParcours.PUBLIE);
            when(parcoursRepository.findByIdAvecEtapes(2L)).thenReturn(Optional.of(parcours));

            ParcoursResponse resultat = service.changerPublication(2L, false);

            assertThat(resultat.statut()).isEqualTo(StatutParcours.BROUILLON);
        }

        @Test
        @DisplayName("leve une exception si le parcours n'existe pas")
        void leveSiIntrouvable() {
            when(parcoursRepository.findByIdAvecEtapes(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.changerPublication(99L, true))
                    .isInstanceOf(RessourceIntrouvableException.class);
        }
    }

    // =================================================================
    @Nested
    @DisplayName("modifier et supprimer")
    class ModifierEtSupprimer {

        @Test
        @DisplayName("met a jour les champs simples")
        void metAJourLesChamps() {
            when(parcoursRepository.findByIdAvecEtapes(1L))
                    .thenReturn(Optional.of(parcoursAvecEtapes()));

            ParcoursRequest requete = new ParcoursRequest(
                    "Titre modifie", "Nouvelle description", Theme.GASTRONOMIQUE,
                    Difficulte.DIFFICILE, 300, null, null, null);

            ParcoursResponse resultat = service.modifier(1L, requete);

            assertThat(resultat.titre()).isEqualTo("Titre modifie");
            assertThat(resultat.theme()).isEqualTo(Theme.GASTRONOMIQUE);
            assertThat(resultat.difficulte()).isEqualTo(Difficulte.DIFFICILE);
        }

        @Test
        @DisplayName("conserve les etapes quand la liste n'est pas fournie")
        void conserveLesEtapesSiListeAbsente() {
            when(parcoursRepository.findByIdAvecEtapes(1L))
                    .thenReturn(Optional.of(parcoursAvecEtapes()));

            ParcoursRequest requete = new ParcoursRequest(
                    "Titre modifie", null, Theme.CULTUREL, Difficulte.FACILE,
                    null, null, null, null);   // etapes = null

            assertThat(service.modifier(1L, requete).etapes()).hasSize(1);
        }

        @Test
        @DisplayName("remplace les etapes quand la liste est fournie")
        void remplaceLesEtapesSiListeFournie() {
            when(parcoursRepository.findByIdAvecEtapes(1L))
                    .thenReturn(Optional.of(parcoursAvecEtapes()));

            ParcoursRequest requete = new ParcoursRequest(
                    "Titre", null, Theme.CULTUREL, Difficulte.FACILE, null, null, null,
                    List.of(etapeRequest("Nouvelle unique", 18.60, -72.40)));

            ParcoursResponse resultat = service.modifier(1L, requete);

            assertThat(resultat.etapes()).hasSize(1);
            assertThat(resultat.etapes().get(0).nom()).isEqualTo("Nouvelle unique");
        }

        @Test
        @DisplayName("supprime un parcours existant")
        void supprimeSiExistant() {
            when(parcoursRepository.existsById(1L)).thenReturn(true);

            service.supprimer(1L);

            verify(parcoursRepository).deleteById(1L);
        }

        @Test
        @DisplayName("leve une exception a la suppression d'un parcours inexistant")
        void leveALaSuppressionSiIntrouvable() {
            when(parcoursRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> service.supprimer(99L))
                    .isInstanceOf(RessourceIntrouvableException.class);

            verify(parcoursRepository, never()).deleteById(anyLong());
        }
    }

    // =================================================================
    @Nested
    @DisplayName("consulterEtComptabiliser")
    class Comptabiliser {

        @Test
        @DisplayName("incremente le compteur de consultations")
        void incrementeLeCompteur() {
            when(parcoursRepository.findByIdAvecEtapes(1L))
                    .thenReturn(Optional.of(parcoursAvecEtapes()));

            service.consulterEtComptabiliser(1L);

            verify(parcoursRepository).incrementerConsultations(1L);
        }
    }
}
