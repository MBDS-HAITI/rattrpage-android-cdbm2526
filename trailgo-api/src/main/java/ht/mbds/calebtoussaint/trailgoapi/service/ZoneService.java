// src/main/java/ht/mbds/calebtoussaint/trailgoapi/service/ZoneService.java
package ht.mbds.calebtoussaint.trailgoapi.service;

import ht.mbds.calebtoussaint.trailgoapi.domain.Parcours;
import ht.mbds.calebtoussaint.trailgoapi.domain.ZoneGeographique;
import ht.mbds.calebtoussaint.trailgoapi.exception.RegleMetierException;
import ht.mbds.calebtoussaint.trailgoapi.exception.RessourceIntrouvableException;
import ht.mbds.calebtoussaint.trailgoapi.repository.ZoneGeographiqueRepository;
import ht.mbds.calebtoussaint.trailgoapi.util.GeoUtils;
import ht.mbds.calebtoussaint.trailgoapi.web.dto.*;
import ht.mbds.calebtoussaint.trailgoapi.web.mapper.ParcoursMapper;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Polygon;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ZoneService {

    private final ZoneGeographiqueRepository zoneRepository;
    private final ParcoursMapper parcoursMapper;

    // =============== LECTURE ===============

    public List<ZoneResponse> lister() {
        return zoneRepository.findAll().stream().map(this::versDto).toList();
    }

    public ZoneResponse consulter(Long id) {
        return versDto(chargerZone(id));
    }

    /** Parcours entierement contenus dans la zone (ST_Within). */
    public List<ParcoursSummaryResponse> parcoursDeLaZone(Long zoneId) {
        chargerZone(zoneId);   // 404 si la zone n'existe pas
        return zoneRepository.parcoursInclus(zoneId).stream()
                .map(parcoursMapper::versResume)
                .toList();
    }

    // =============== ECRITURE ===============

    @Transactional
    public ZoneResponse creer(ZoneRequest requete) {
        ZoneGeographique zone = ZoneGeographique.builder()
                .nom(requete.nom().trim())
                .regionAdministrative(requete.regionAdministrative())
                .polygone(construirePolygone(requete.polygone()))
                .build();

        ZoneGeographique enregistree = zoneRepository.saveAndFlush(zone);

        // Une nouvelle zone peut englober des parcours deja existants :
        // on relance le rattachement pour tous.
        zoneRepository.rattacherTousLesParcours();

        return versDto(enregistree);
    }

    @Transactional
    public ZoneResponse modifier(Long id, ZoneRequest requete) {
        ZoneGeographique zone = chargerZone(id);

        zone.setNom(requete.nom().trim());
        zone.setRegionAdministrative(requete.regionAdministrative());
        zone.setPolygone(construirePolygone(requete.polygone()));

        zoneRepository.saveAndFlush(zone);

        // Le contour a change : les rattachements peuvent ne plus etre valides.
        zoneRepository.rattacherTousLesParcours();

        return versDto(zone);
    }

    @Transactional
    public void supprimer(Long id) {
        ZoneGeographique zone = chargerZone(id);
        zoneRepository.delete(zone);
        // La contrainte ON DELETE SET NULL detache automatiquement
        // les parcours qui referencaient cette zone.
    }

    /** Relance le rattachement automatique sur l'ensemble des parcours. */
    @Transactional
    public int rattacherTousLesParcours() {
        return zoneRepository.rattacherTousLesParcours();
    }

    // =============== CONVERSIONS ===============

    /**
     * Construit un Polygon JTS a partir du GeoJSON recu.
     *
     * Verifie les trois conditions d'un anneau valide :
     *   - au moins 4 positions (3 sommets + le point de fermeture)
     *   - coordonnees dans les bornes
     *   - anneau ferme (dernier point identique au premier)
     *
     * Si l'anneau n'est pas ferme, on le ferme automatiquement plutot
     * que de rejeter : beaucoup d'outils de dessin cartographique
     * omettent le point de fermeture.
     */
    private Polygon construirePolygone(GeoJsonPolygon geoJson) {
        if (!"Polygon".equalsIgnoreCase(geoJson.type())) {
            throw new RegleMetierException(
                    "Seul le type Polygon est accepte, recu : " + geoJson.type());
        }
        List<List<Double>> anneau = geoJson.coordinates().get(0);

        if (anneau == null || anneau.size() < 4) {
            throw new RegleMetierException(
                    "Un polygone doit comporter au moins 4 positions "
                    + "(3 sommets plus le point de fermeture)");
        }

        List<Coordinate> coordonnees = new ArrayList<>();
        for (List<Double> position : anneau) {
            if (position == null || position.size() < 2
                    || position.get(0) == null || position.get(1) == null) {
                throw new RegleMetierException(
                        "Chaque position doit etre une paire [longitude, latitude]");
            }
            double longitude = position.get(0);
            double latitude = position.get(1);

            if (latitude < -90 || latitude > 90) {
                throw new RegleMetierException(
                        "Latitude hors bornes : %s. GeoJSON attend "
                        + "[longitude, latitude].".formatted(latitude));
            }
            if (longitude < -180 || longitude > 180) {
                throw new RegleMetierException("Longitude hors bornes : " + longitude);
            }
            coordonnees.add(new Coordinate(longitude, latitude));
        }

        // Fermeture automatique si le dernier point differe du premier.
        Coordinate premier = coordonnees.get(0);
        Coordinate dernier = coordonnees.get(coordonnees.size() - 1);
        if (!premier.equals2D(dernier)) {
            coordonnees.add(new Coordinate(premier));
        }

        try {
            return GeoUtils.factory().createPolygon(
                    coordonnees.toArray(new Coordinate[0]));
        } catch (IllegalArgumentException ex) {
            throw new RegleMetierException("Polygone invalide : " + ex.getMessage());
        }
    }

    private ZoneResponse versDto(ZoneGeographique zone) {
        Polygon polygone = zone.getPolygone();

        List<List<Double>> anneau = new ArrayList<>();
        for (Coordinate c : polygone.getExteriorRing().getCoordinates()) {
            anneau.add(List.of(c.getX(), c.getY()));   // [longitude, latitude]
        }

        Envelope enveloppe = polygone.getEnvelopeInternal();
        List<Double> bbox = List.of(
                enveloppe.getMinX(), enveloppe.getMinY(),
                enveloppe.getMaxX(), enveloppe.getMaxY());

        Double superficie = zoneRepository.calculerSuperficieKm2(zone.getId());
        long nbParcours = zoneRepository.parcoursInclus(zone.getId()).size();

        return new ZoneResponse(
                zone.getId(),
                zone.getNom(),
                zone.getRegionAdministrative(),
                new GeoJsonPolygon("Polygon", List.of(anneau)),
                superficie == null ? null
                        : BigDecimal.valueOf(superficie).setScale(3, RoundingMode.HALF_UP),
                bbox,
                nbParcours);
    }

    private ZoneGeographique chargerZone(Long id) {
        return zoneRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Zone geographique", id));
    }
}
