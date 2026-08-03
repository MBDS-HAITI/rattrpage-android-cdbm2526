// src/main/java/ht/mbds/calebtoussaint/trailgoapi/web/mapper/ParcoursMapper.java
package ht.mbds.calebtoussaint.trailgoapi.web.mapper;

import ht.mbds.calebtoussaint.trailgoapi.domain.Etape;
import ht.mbds.calebtoussaint.trailgoapi.domain.Parcours;
import ht.mbds.calebtoussaint.trailgoapi.util.GeoUtils;
import ht.mbds.calebtoussaint.trailgoapi.web.dto.*;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Conversion entites <-> DTO.
 *
 * Ecrit a la main volontairement : c'est ici que se fait la traduction
 * entre la geometrie PostGIS et le couple latitude/longitude, et c'est
 * la seule zone du projet ou l'inversion X/Y peut poser probleme.
 * Autant qu'elle soit visible et posable en point d'arret.
 *
 * RAPPEL : point.getY() = LATITUDE, point.getX() = LONGITUDE.
 */
@Component
public class ParcoursMapper {

    // ---------------- ETAPE ----------------

    public EtapeResponse versDto(Etape etape) {
        return new EtapeResponse(
                etape.getId(),
                etape.getNom(),
                etape.getDescription(),
                etape.getPosition() == null ? null : etape.getPosition().getY(),
                etape.getPosition() == null ? null : etape.getPosition().getX(),
                etape.getOrdre(),
                etape.getPhoto(),
                etape.getDureeVisiteMin()
        );
    }

    public List<EtapeResponse> versDtosEtapes(List<Etape> etapes) {
        return etapes == null ? List.of() : etapes.stream().map(this::versDto).toList();
    }

    /** Cree une entite Etape. L'ordre et le parcours sont poses par le service. */
    public Etape versEntite(EtapeRequest requete) {
        Etape etape = new Etape();
        etape.setNom(requete.nom());
        etape.setDescription(requete.description());
        etape.setPosition(GeoUtils.point(requete.latitude(), requete.longitude()));
        etape.setPhoto(requete.photo());
        etape.setDureeVisiteMin(requete.dureeVisiteMin());
        return etape;
    }

    /** Mise a jour d'une etape existante : l'ordre n'est pas touche. */
    public void mettreAJour(EtapeRequest requete, Etape etape) {
        etape.setNom(requete.nom());
        etape.setDescription(requete.description());
        etape.setPosition(GeoUtils.point(requete.latitude(), requete.longitude()));
        etape.setPhoto(requete.photo());
        etape.setDureeVisiteMin(requete.dureeVisiteMin());
    }

    // ---------------- PARCOURS ----------------

    public ParcoursResponse versDto(Parcours parcours) {
        return new ParcoursResponse(
                parcours.getId(),
                parcours.getTitre(),
                parcours.getDescription(),
                parcours.getTheme(),
                parcours.getDifficulte(),
                parcours.getDureeEstimeeMin(),
                parcours.getImageCouverture(),
                parcours.getStatut(),
                parcours.getDistanceTotaleKm(),
                parcours.getZone() == null ? null : parcours.getZone().getId(),
                parcours.getZone() == null ? null : parcours.getZone().getNom(),
                parcours.getNbConsultations(),
                parcours.getDateCreation(),
                parcours.getDateModification(),
                versDtosEtapes(parcours.getEtapes())
        );
    }

    public ParcoursSummaryResponse versResume(Parcours parcours) {
        return new ParcoursSummaryResponse(
                parcours.getId(),
                parcours.getTitre(),
                parcours.getTheme(),
                parcours.getDifficulte(),
                parcours.getDureeEstimeeMin(),
                parcours.getImageCouverture(),
                parcours.getStatut(),
                parcours.getDistanceTotaleKm(),
                parcours.getEtapes() == null ? 0 : parcours.getEtapes().size()
        );
    }

    /**
     * Cree une entite Parcours a partir de la requete.
     * Les etapes, la zone et le statut sont geres par le service.
     */
    public Parcours versEntite(ParcoursRequest requete) {
        Parcours parcours = new Parcours();
        appliquer(requete, parcours);
        return parcours;
    }

    /** Champs communs a la creation et a la modification. */
    public void appliquer(ParcoursRequest requete, Parcours parcours) {
        parcours.setTitre(requete.titre());
        parcours.setDescription(requete.description());
        parcours.setTheme(requete.theme());
        parcours.setDifficulte(requete.difficulte());
        parcours.setDureeEstimeeMin(requete.dureeEstimeeMin());
        parcours.setImageCouverture(requete.imageCouverture());
    }
}
