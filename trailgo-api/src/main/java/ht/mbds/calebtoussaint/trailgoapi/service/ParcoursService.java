// src/main/java/ht/mbds/calebtoussaint/trailgoapi/service/ParcoursService.java
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
import ht.mbds.calebtoussaint.trailgoapi.web.dto.*;
import ht.mbds.calebtoussaint.trailgoapi.web.mapper.ParcoursMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Logique metier des parcours.
 *
 * C'est la couche que le sujet demande de couvrir a 70 % par des tests.
 * Elle ne connait rien du protocole HTTP : pas de ResponseEntity, pas de
 * requete servlet. C'est ce qui la rend facile a tester.
 *
 * @RequiredArgsConstructor (Lombok) genere le constructeur avec les
 * champs "final". Spring y injecte automatiquement les dependances.
 *
 * @Transactional(readOnly = true) au niveau de la classe : toutes les
 * methodes sont en lecture seule par defaut, sauf celles qui redeclarent
 * @Transactional pour ecrire.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParcoursService {

    private final ParcoursRepository parcoursRepository;
    private final ZoneGeographiqueRepository zoneRepository;
    private final ParcoursMapper mapper;

    // =============== LECTURE ===============

    public PageResponse<ParcoursSummaryResponse> rechercher(Theme theme,
                                                            Difficulte difficulte,
                                                            StatutParcours statut,
                                                            Integer dureeMax,
                                                            String recherche,
                                                            Pageable pageable) {

        // POINT CRITIQUE : ce motif ne doit JAMAIS valoir null.
        //
        // 1. En SQL, "titre LIKE null" ne vaut pas "vrai" mais "inconnu",
        //    ce qui exclut toutes les lignes : la liste reviendrait vide.
        // 2. Un parametre String null empeche aussi PostgreSQL de deduire
        //    son type et provoque l'erreur "function lower(bytea) does not exist".
        //
        // "%" est le joker SQL : la condition est alors toujours vraie.
        String motif = (recherche == null || recherche.isBlank())
                ? "%"
                : "%" + recherche.trim().toLowerCase() + "%";

        Page<Parcours> page = parcoursRepository.rechercher(
                theme, difficulte, statut, dureeMax, motif, pageable);

        return PageResponse.de(page, mapper::versResume);
    }

    public ParcoursResponse consulter(Long id) {
        Parcours parcours = parcoursRepository.findByIdAvecEtapes(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Parcours", id));
        return mapper.versDto(parcours);
    }

    /** Consultation publique : incremente le compteur du tableau de bord. */
    @Transactional
    public ParcoursResponse consulterEtComptabiliser(Long id) {
        ParcoursResponse reponse = consulter(id);
        parcoursRepository.incrementerConsultations(id);
        return reponse;
    }

    // =============== ECRITURE ===============

    @Transactional
    public ParcoursResponse creer(ParcoursRequest requete) {
        Parcours parcours = mapper.versEntite(requete);
        parcours.setStatut(StatutParcours.BROUILLON);
        parcours.setZone(resoudreZone(requete.zoneId()));

        remplacerEtapes(parcours, requete.etapes());

        return mapper.versDto(parcoursRepository.save(parcours));
    }

    @Transactional
    public ParcoursResponse modifier(Long id, ParcoursRequest requete) {
        Parcours parcours = parcoursRepository.findByIdAvecEtapes(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Parcours", id));

        mapper.appliquer(requete, parcours);

        if (requete.zoneId() != null) {
            parcours.setZone(resoudreZone(requete.zoneId()));
        }
        // Si le client fournit une liste d'etapes, elle remplace l'ancienne.
        // S'il ne la fournit pas (null), les etapes existantes sont conservees.
        if (requete.etapes() != null) {
            parcours.getEtapes().clear();
            remplacerEtapes(parcours, requete.etapes());
        }
        return mapper.versDto(parcours);
    }

    @Transactional
    public void supprimer(Long id) {
        if (!parcoursRepository.existsById(id)) {
            throw new RessourceIntrouvableException("Parcours", id);
        }
        parcoursRepository.deleteById(id);
    }

    /**
     * Publication ou depublication.
     *
     * Regle metier : on refuse de publier un parcours sans etape. Un
     * touriste qui ouvrirait un parcours vide verrait une carte blanche ;
     * mieux vaut bloquer cote serveur que se reposer sur l'interface.
     */
    @Transactional
    public ParcoursResponse changerPublication(Long id, boolean publier) {
        Parcours parcours = parcoursRepository.findByIdAvecEtapes(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Parcours", id));

        if (publier && parcours.getEtapes().isEmpty()) {
            throw new RegleMetierException("Impossible de publier un parcours sans etape");
        }
        parcours.setStatut(publier ? StatutParcours.PUBLIE : StatutParcours.BROUILLON);
        return mapper.versDto(parcours);
    }

    // =============== OUTILS INTERNES ===============

    /**
     * Remplace la liste d'etapes et renumerote l'ordre de 1 a N.
     *
     * On ignore volontairement tout ordre fourni par le client :
     * renumeroter garantit qu'il n'y a ni trou ni doublon, et donc que la
     * contrainte d'unicite (parcours_id, ordre) ne peut pas sauter.
     */
    private void remplacerEtapes(Parcours parcours, List<EtapeRequest> requetes) {
        if (requetes == null || requetes.isEmpty()) {
            return;
        }
        int ordre = 1;
        for (EtapeRequest requete : requetes) {
            Etape etape = mapper.versEntite(requete);
            etape.setOrdre(ordre++);
            parcours.ajouterEtape(etape);
        }
    }

    private ZoneGeographique resoudreZone(Long zoneId) {
        if (zoneId == null) {
            return null;
        }
        return zoneRepository.findById(zoneId)
                .orElseThrow(() -> new RessourceIntrouvableException("Zone geographique", zoneId));
    }
}