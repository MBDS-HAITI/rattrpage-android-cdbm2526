// src/main/java/ht/mbds/calebtoussaint/trailgoapi/service/AvisService.java
package ht.mbds.calebtoussaint.trailgoapi.service;

import ht.mbds.calebtoussaint.trailgoapi.domain.Avis;
import ht.mbds.calebtoussaint.trailgoapi.domain.Parcours;
import ht.mbds.calebtoussaint.trailgoapi.domain.Utilisateur;
import ht.mbds.calebtoussaint.trailgoapi.domain.enums.Role;
import ht.mbds.calebtoussaint.trailgoapi.domain.enums.StatutParcours;
import ht.mbds.calebtoussaint.trailgoapi.exception.RegleMetierException;
import ht.mbds.calebtoussaint.trailgoapi.exception.RessourceIntrouvableException;
import ht.mbds.calebtoussaint.trailgoapi.repository.AvisRepository;
import ht.mbds.calebtoussaint.trailgoapi.repository.ParcoursRepository;
import ht.mbds.calebtoussaint.trailgoapi.repository.UtilisateurRepository;
import ht.mbds.calebtoussaint.trailgoapi.repository.projection.StatistiqueAvis;
import ht.mbds.calebtoussaint.trailgoapi.web.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Gestion des avis.
 *
 * REGLES METIER APPLIQUEES ICI :
 *   - un seul avis par utilisateur et par parcours
 *   - on ne note que les parcours publies
 *   - seul l'auteur modifie son avis ; l'auteur ou un ADMIN le supprime
 *   - tout utilisateur connecte peut signaler un avis
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AvisService {

    private final AvisRepository avisRepository;
    private final ParcoursRepository parcoursRepository;
    private final UtilisateurRepository utilisateurRepository;

    // =============== LECTURE ===============

    public PageResponse<AvisResponse> listerParParcours(Long parcoursId, Pageable pageable) {
        verifierParcoursExiste(parcoursId);
        return PageResponse.de(avisRepository.findByParcoursId(parcoursId, pageable),
                               this::versDto);
    }

    /** Avis signales : alimente l'ecran de moderation du back office. */
    public PageResponse<AvisResponse> listerSignales(Pageable pageable) {
        return PageResponse.de(avisRepository.findSignales(pageable), this::versDto);
    }

    public StatistiquesAvisResponse statistiques(Long parcoursId) {
        verifierParcoursExiste(parcoursId);

        List<StatistiqueAvis> agregat = avisRepository.agregerParParcours(List.of(parcoursId));

        Double moyenne = null;
        long total = 0;
        if (!agregat.isEmpty()) {
            moyenne = arrondir(agregat.get(0).getMoyenne());
            total = agregat.get(0).getTotal();
        }

        // Repartition initialisee a zero pour les 5 notes : le graphique
        // du back office a besoin de toutes les barres, meme vides.
        Map<Short, Long> repartition = new LinkedHashMap<>();
        for (short note = 1; note <= 5; note++) {
            repartition.put(note, 0L);
        }
        for (Object[] ligne : avisRepository.repartitionDesNotes(parcoursId)) {
            repartition.put((Short) ligne[0], (Long) ligne[1]);
        }

        return new StatistiquesAvisResponse(parcoursId, moyenne, total, repartition);
    }

    // =============== ECRITURE ===============

    @Transactional
    public AvisResponse deposer(Long parcoursId, AvisRequest requete, String emailAuteur) {
        Parcours parcours = parcoursRepository.findById(parcoursId)
                .orElseThrow(() -> new RessourceIntrouvableException("Parcours", parcoursId));

        if (parcours.getStatut() != StatutParcours.PUBLIE) {
            throw new RegleMetierException(
                    "Seuls les parcours publies peuvent recevoir un avis");
        }

        Utilisateur auteur = chargerUtilisateur(emailAuteur);

        // La base porte deja une contrainte d'unicite ; on la verifie ici
        // pour renvoyer un message clair plutot qu'une erreur SQL brute.
        if (avisRepository.existsByParcoursIdAndAuteurId(parcoursId, auteur.getId())) {
            throw new RegleMetierException(
                    "Vous avez deja laisse un avis sur ce parcours. "
                    + "Utilisez PUT pour le modifier.");
        }

        Avis avis = Avis.builder()
                .parcours(parcours)
                .auteur(auteur)
                .note(requete.note())
                .commentaire(requete.commentaire())
                .signale(false)
                .build();

        return versDto(avisRepository.save(avis));
    }

    @Transactional
    public AvisResponse modifier(Long avisId, AvisRequest requete, String emailAuteur) {
        Avis avis = chargerAvis(avisId);
        Utilisateur utilisateur = chargerUtilisateur(emailAuteur);

        // Un ADMIN peut supprimer un avis mais PAS le reecrire :
        // modifier les mots d'autrui serait une falsification.
        if (!avis.getAuteur().getId().equals(utilisateur.getId())) {
            throw new AccessDeniedException("Vous ne pouvez modifier que vos propres avis");
        }

        avis.setNote(requete.note());
        avis.setCommentaire(requete.commentaire());
        return versDto(avis);
    }

    @Transactional
    public void supprimer(Long avisId, String emailDemandeur) {
        Avis avis = chargerAvis(avisId);
        Utilisateur utilisateur = chargerUtilisateur(emailDemandeur);

        boolean estAuteur = avis.getAuteur().getId().equals(utilisateur.getId());
        boolean estAdmin = utilisateur.getRole() == Role.ADMIN;

        if (!estAuteur && !estAdmin) {
            throw new AccessDeniedException(
                    "Seuls l'auteur ou un administrateur peuvent supprimer cet avis");
        }
        avisRepository.delete(avis);
    }

    /** Signalement par un utilisateur : l'avis remonte en moderation. */
    @Transactional
    public AvisResponse signaler(Long avisId) {
        Avis avis = chargerAvis(avisId);
        avis.setSignale(true);
        return versDto(avis);
    }

    /** Levee du signalement par un administrateur. */
    @Transactional
    public AvisResponse leverSignalement(Long avisId) {
        Avis avis = chargerAvis(avisId);
        avis.setSignale(false);
        return versDto(avis);
    }

    // =============== INTERNE ===============

    private AvisResponse versDto(Avis avis) {
        return new AvisResponse(
                avis.getId(),
                avis.getParcours().getId(),
                avis.getParcours().getTitre(),
                avis.getNote(),
                avis.getCommentaire(),
                avis.getAuteur().getNom(),
                avis.getAuteur().getId(),
                avis.getDateCreation(),
                avis.isSignale());
    }

    private Avis chargerAvis(Long id) {
        return avisRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Avis", id));
    }

    private Utilisateur chargerUtilisateur(String email) {
        return utilisateurRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur", email));
    }

    private void verifierParcoursExiste(Long parcoursId) {
        if (!parcoursRepository.existsById(parcoursId)) {
            throw new RessourceIntrouvableException("Parcours", parcoursId);
        }
    }

    private Double arrondir(Double valeur) {
        return valeur == null ? null : Math.round(valeur * 10d) / 10d;
    }
}
