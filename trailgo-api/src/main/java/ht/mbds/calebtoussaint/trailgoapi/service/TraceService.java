// src/main/java/ht/mbds/calebtoussaint/trailgoapi/service/TraceService.java
package ht.mbds.calebtoussaint.trailgoapi.service;

import ht.mbds.calebtoussaint.trailgoapi.domain.Parcours;
import ht.mbds.calebtoussaint.trailgoapi.exception.RegleMetierException;
import ht.mbds.calebtoussaint.trailgoapi.exception.RessourceIntrouvableException;
import ht.mbds.calebtoussaint.trailgoapi.repository.ParcoursRepository;
import ht.mbds.calebtoussaint.trailgoapi.util.GeoUtils;
import ht.mbds.calebtoussaint.trailgoapi.util.GpxParser;
import ht.mbds.calebtoussaint.trailgoapi.web.dto.GeoJsonLineString;
import ht.mbds.calebtoussaint.trailgoapi.web.dto.TraceResponse;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.LineString;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Import et export du trace cartographique d'un parcours.
 *
 * POINT CENTRAL DU PROJET, a savoir expliquer en soutenance :
 *
 *   ST_Length(trace)              -> renvoie des DEGRES
 *   ST_Length(trace::geography)   -> renvoie des METRES
 *
 * En SRID 4326 les coordonnees sont des angles, pas des distances.
 * PostGIS calcule alors une longueur "plate" en degres, denuee de sens
 * physique. Le cast en geography force un calcul geodesique sur
 * l'ellipsoide terrestre, qui donne bien des metres.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TraceService {

    private final ParcoursRepository parcoursRepository;

    // =============== IMPORT ===============

    /** Import depuis un fichier GPX (format des GPS et applications de rando). */
    @Transactional
    public TraceResponse importerGpx(Long parcoursId, MultipartFile fichier) {
        if (fichier == null || fichier.isEmpty()) {
            throw new RegleMetierException("Le fichier GPX est vide");
        }
        String nom = fichier.getOriginalFilename();
        if (nom != null && !nom.toLowerCase().endsWith(".gpx")) {
            throw new RegleMetierException("Le fichier doit porter l'extension .gpx");
        }

        List<Coordinate> coordonnees;
        try {
            coordonnees = GpxParser.lireCoordonnees(fichier.getInputStream());
        } catch (IOException ex) {
            throw new RegleMetierException("Impossible de lire le fichier envoye");
        }
        return enregistrerTrace(parcoursId, coordonnees);
    }

    /** Import depuis une geometrie GeoJSON envoyee par React ou Android. */
    @Transactional
    public TraceResponse importerGeoJson(Long parcoursId, GeoJsonLineString geoJson) {

        if (!"LineString".equalsIgnoreCase(geoJson.type())) {
            throw new RegleMetierException(
                    "Seul le type LineString est accepte, recu : " + geoJson.type());
        }
        if (geoJson.coordinates().size() < 2) {
            throw new RegleMetierException("Un trace doit comporter au moins 2 points");
        }

        List<Coordinate> coordonnees = new ArrayList<>();
        for (List<Double> paire : geoJson.coordinates()) {

            if (paire == null || paire.size() < 2
                    || paire.get(0) == null || paire.get(1) == null) {
                throw new RegleMetierException(
                        "Chaque coordonnee doit etre une paire [longitude, latitude]");
            }
            // RFC 7946 : coordinates = [longitude, latitude]
            double longitude = paire.get(0);
            double latitude = paire.get(1);

            if (latitude < -90 || latitude > 90) {
                throw new RegleMetierException(
                        "Latitude hors bornes : %s. Verifiez l'ordre des coordonnees, "
                        + "GeoJSON attend [longitude, latitude].".formatted(latitude));
            }
            if (longitude < -180 || longitude > 180) {
                throw new RegleMetierException("Longitude hors bornes : " + longitude);
            }
            coordonnees.add(new Coordinate(longitude, latitude));
        }
        return enregistrerTrace(parcoursId, coordonnees);
    }

    // =============== EXPORT ===============

    public TraceResponse consulterTrace(Long parcoursId) {
        Parcours parcours = parcoursRepository.findById(parcoursId)
                .orElseThrow(() -> new RessourceIntrouvableException("Parcours", parcoursId));

        if (parcours.getTrace() == null) {
            throw new RessourceIntrouvableException(
                    "Aucun trace enregistre pour le parcours " + parcoursId);
        }
        return construireReponse(parcours);
    }

    /** Suppression du trace : le parcours redevient non publiable. */
    @Transactional
    public void supprimerTrace(Long parcoursId) {
        Parcours parcours = parcoursRepository.findById(parcoursId)
                .orElseThrow(() -> new RessourceIntrouvableException("Parcours", parcoursId));

        parcours.setTrace(null);
        parcours.setBbox(null);
        parcours.setDistanceTotaleKm(null);
    }

    // =============== INTERNE ===============

    private TraceResponse enregistrerTrace(Long parcoursId, List<Coordinate> coordonnees) {
        Parcours parcours = parcoursRepository.findById(parcoursId)
                .orElseThrow(() -> new RessourceIntrouvableException("Parcours", parcoursId));

        LineString trace = GeoUtils.lineString(coordonnees);
        parcours.setTrace(trace);
        parcours.setBbox(GeoUtils.boundingBox(trace));

        // flush() : force l'ecriture du trace en base AVANT d'appeler
        // PostGIS. Sans cela la requete ST_Length lirait l'ancienne
        // valeur, voire aucune valeur.
        parcoursRepository.saveAndFlush(parcours);

        Double distanceKm = parcoursRepository.calculerDistanceKm(parcoursId);
        parcours.setDistanceTotaleKm(arrondir(distanceKm));

        return construireReponse(parcours);
    }

    private TraceResponse construireReponse(Parcours parcours) {
        LineString trace = parcours.getTrace();

        List<List<Double>> coordonnees = new ArrayList<>();
        for (Coordinate c : trace.getCoordinates()) {
            // Sortie en GeoJSON : [longitude, latitude]
            coordonnees.add(List.of(c.getX(), c.getY()));
        }

        Envelope enveloppe = trace.getEnvelopeInternal();
        List<Double> bbox = List.of(
                enveloppe.getMinX(),   // ouest
                enveloppe.getMinY(),   // sud
                enveloppe.getMaxX(),   // est
                enveloppe.getMaxY());  // nord

        return new TraceResponse(
                parcours.getId(),
                new GeoJsonLineString("LineString", coordonnees),
                parcours.getDistanceTotaleKm(),
                trace.getNumPoints(),
                bbox);
    }

    private BigDecimal arrondir(Double valeur) {
        return valeur == null ? null
                : BigDecimal.valueOf(valeur).setScale(3, RoundingMode.HALF_UP);
    }
}
