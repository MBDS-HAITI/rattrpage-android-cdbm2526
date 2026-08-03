// src/main/java/ht/mbds/calebtoussaint/trailgoapi/service/RechercheSpatialeService.java
package ht.mbds.calebtoussaint.trailgoapi.service;

import ht.mbds.calebtoussaint.trailgoapi.domain.enums.CategoriePoi;
import ht.mbds.calebtoussaint.trailgoapi.domain.enums.Difficulte;
import ht.mbds.calebtoussaint.trailgoapi.domain.enums.Theme;
import ht.mbds.calebtoussaint.trailgoapi.exception.RegleMetierException;
import ht.mbds.calebtoussaint.trailgoapi.repository.RechercheSpatialeRepository;
import ht.mbds.calebtoussaint.trailgoapi.web.dto.EtapeProcheResponse;
import ht.mbds.calebtoussaint.trailgoapi.web.dto.ParcoursProcheResponse;
import ht.mbds.calebtoussaint.trailgoapi.web.dto.PoiProcheResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Recherches geographiques.
 *
 * Les bornes sont verifiees ici plutot que par des annotations : les
 * messages d'erreur sont ainsi explicites et coherents avec le reste
 * de l'API (409 + ProblemDetail).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RechercheSpatialeService {

    /** Au-dela, la requete perd son interet et charge inutilement la base. */
    private static final double RAYON_MAX_METRES = 100_000;   // 100 km
    private static final int LIMITE_PAR_DEFAUT = 50;
    private static final int LIMITE_MAX = 200;

    private final RechercheSpatialeRepository rechercheRepository;

    // =============== PARCOURS ===============

    public List<ParcoursProcheResponse> parcoursProches(Double latitude,
                                                        Double longitude,
                                                        Double rayonMetres,
                                                        Integer limite) {
        verifierPosition(latitude, longitude);
        double rayon = verifierRayon(rayonMetres);
        int max = normaliserLimite(limite);

        return rechercheRepository
                .parcoursDansRayon(latitude, longitude, rayon, max)
                .stream()
                .map(p -> new ParcoursProcheResponse(
                        p.getId(), p.getTitre(),
                        Theme.valueOf(p.getTheme()),
                        Difficulte.valueOf(p.getDifficulte()),
                        p.getDureeEstimeeMin(), p.getImageCouverture(),
                        p.getDistanceTotaleKm(),
                        arrondir(p.getDistanceM())))
                .toList();
    }

    /**
     * Recherche par rectangle.
     *
     * @param ouest longitude minimale, sud latitude minimale,
     *              est longitude maximale, nord latitude maximale.
     *              C'est l'ordre de ST_MakeEnvelope et celui que
     *              Leaflet renvoie via map.getBounds().toBBoxString().
     */
    public List<ParcoursProcheResponse> parcoursDansRectangle(Double ouest, Double sud,
                                                              Double est, Double nord,
                                                              Integer limite) {
        if (ouest == null || sud == null || est == null || nord == null) {
            throw new RegleMetierException(
                    "Les quatre bornes (ouest, sud, est, nord) sont obligatoires");
        }
        verifierPosition(sud, ouest);
        verifierPosition(nord, est);

        if (ouest >= est) {
            throw new RegleMetierException(
                    "La borne ouest doit etre inferieure a la borne est");
        }
        if (sud >= nord) {
            throw new RegleMetierException(
                    "La borne sud doit etre inferieure a la borne nord");
        }
        int max = normaliserLimite(limite);

        return rechercheRepository
                .parcoursDansRectangle(ouest, sud, est, nord, max)
                .stream()
                .map(p -> new ParcoursProcheResponse(
                        p.getId(), p.getTitre(),
                        Theme.valueOf(p.getTheme()),
                        Difficulte.valueOf(p.getDifficulte()),
                        p.getDureeEstimeeMin(), p.getImageCouverture(),
                        p.getDistanceTotaleKm(),
                        null))   // pas de distance pour une recherche par zone
                .toList();
    }

    // =============== ETAPES ===============

    public List<EtapeProcheResponse> etapesProches(Double latitude,
                                                   Double longitude,
                                                   Double rayonMetres,
                                                   Integer limite) {
        verifierPosition(latitude, longitude);
        double rayon = verifierRayon(rayonMetres);
        int max = normaliserLimite(limite);

        return rechercheRepository
                .etapesDansRayon(latitude, longitude, rayon, max)
                .stream()
                .map(e -> new EtapeProcheResponse(
                        e.getId(), e.getNom(), e.getDescription(),
                        e.getLatitude(), e.getLongitude(),
                        e.getOrdre(), e.getPhoto(),
                        e.getParcoursId(), e.getParcoursTitre(),
                        arrondir(e.getDistanceM())))
                .toList();
    }

    // =============== POINTS D'INTERET ===============

    public List<PoiProcheResponse> poisProches(Double latitude,
                                               Double longitude,
                                               Double rayonMetres,
                                               Integer limite) {
        verifierPosition(latitude, longitude);
        double rayon = verifierRayon(rayonMetres);
        int max = normaliserLimite(limite);

        return rechercheRepository
                .poisDansRayon(latitude, longitude, rayon, max)
                .stream()
                .map(poi -> new PoiProcheResponse(
                        poi.getId(), poi.getTitre(),
                        CategoriePoi.valueOf(poi.getCategorie()),
                        poi.getAdresse(),
                        poi.getLatitude(), poi.getLongitude(),
                        poi.getRayonProximiteM(),
                        arrondir(poi.getDistanceM())))
                .toList();
    }

    // =============== VERIFICATIONS ===============

    private void verifierPosition(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            throw new RegleMetierException("La latitude et la longitude sont obligatoires");
        }
        if (latitude < -90 || latitude > 90) {
            throw new RegleMetierException(
                    "Latitude invalide : %s (attendu entre -90 et 90)".formatted(latitude));
        }
        if (longitude < -180 || longitude > 180) {
            throw new RegleMetierException(
                    "Longitude invalide : %s (attendu entre -180 et 180)".formatted(longitude));
        }
    }

    private double verifierRayon(Double rayonMetres) {
        if (rayonMetres == null) {
            return 5_000;   // 5 km par defaut
        }
        if (rayonMetres <= 0) {
            throw new RegleMetierException("Le rayon doit etre strictement positif");
        }
        if (rayonMetres > RAYON_MAX_METRES) {
            throw new RegleMetierException(
                    "Rayon trop grand : %.0f m (maximum %.0f m)"
                            .formatted(rayonMetres, RAYON_MAX_METRES));
        }
        return rayonMetres;
    }

    private int normaliserLimite(Integer limite) {
        if (limite == null || limite <= 0) {
            return LIMITE_PAR_DEFAUT;
        }
        return Math.min(limite, LIMITE_MAX);
    }

    /** Distances au decimetre : la precision GPS ne justifie pas mieux. */
    private Double arrondir(Double valeur) {
        return valeur == null ? null : Math.round(valeur * 10d) / 10d;
    }
}
