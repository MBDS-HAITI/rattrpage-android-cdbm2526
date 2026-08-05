// src/main/java/ht/mbds/calebtoussaint/trailgoapi/service/UtilisateurAdminService.java
package ht.mbds.calebtoussaint.trailgoapi.service;

import ht.mbds.calebtoussaint.trailgoapi.domain.Utilisateur;
import ht.mbds.calebtoussaint.trailgoapi.domain.enums.Role;
import ht.mbds.calebtoussaint.trailgoapi.exception.RegleMetierException;
import ht.mbds.calebtoussaint.trailgoapi.exception.RessourceIntrouvableException;
import ht.mbds.calebtoussaint.trailgoapi.repository.UtilisateurRepository;
import ht.mbds.calebtoussaint.trailgoapi.web.dto.ModifierUtilisateurRequest;
import ht.mbds.calebtoussaint.trailgoapi.web.dto.PageResponse;
import ht.mbds.calebtoussaint.trailgoapi.web.dto.UtilisateurAdminResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Administration des comptes utilisateurs : liste, changement de role,
 * activation/desactivation.
 *
 * Reserve aux ADMIN (verifie par SecurityConfig, pas ici : cette classe
 * ne connait pas le concept HTTP, elle applique seulement les regles
 * metier internes).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UtilisateurAdminService {

    private final UtilisateurRepository utilisateurRepository;

    public PageResponse<UtilisateurAdminResponse> lister(Pageable pageable) {
        Page<Utilisateur> page = utilisateurRepository.findAll(pageable);
        return PageResponse.de(page, this::versDto);
    }

    /**
     * Modifie le role et le statut actif d'un utilisateur.
     *
     * REGLE DE SECURITE : un administrateur ne peut pas modifier son
     * propre compte via cet endpoint. Sans ce garde-fou, un admin
     * pourrait se retirer ses propres droits ou se desactiver et se
     * retrouver bloque hors de l'application, sans personne pour
     * annuler l'operation.
     */
    @Transactional
    public UtilisateurAdminResponse modifier(Long id, ModifierUtilisateurRequest requete,
                                             String emailDemandeur) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur", id));

        if (utilisateur.getEmail().equalsIgnoreCase(emailDemandeur)) {
            throw new RegleMetierException(
                    "Vous ne pouvez pas modifier votre propre compte depuis cet ecran");
        }

        // Regle complementaire : empecher de desactiver ou retrograder
        // le DERNIER administrateur actif. Sans ce controle, une suite
        // malencontreuse d'actions pourrait laisser la plateforme sans
        // aucun administrateur capable de la gerer.
        boolean perdSonStatutAdmin = utilisateur.getRole() == Role.ADMIN
                && (requete.role() != Role.ADMIN || !requete.actif());

        if (perdSonStatutAdmin) {
            long nbAutresAdminsActifs = utilisateurRepository.countByRoleAndActifTrue(Role.ADMIN) - 1;
            if (nbAutresAdminsActifs < 1) {
                throw new RegleMetierException(
                        "Impossible de retirer le dernier administrateur actif de la plateforme");
            }
        }

        utilisateur.setRole(requete.role());
        utilisateur.setActif(requete.actif());

        return versDto(utilisateur);
    }

    private UtilisateurAdminResponse versDto(Utilisateur utilisateur) {
        return new UtilisateurAdminResponse(
                utilisateur.getId(),
                utilisateur.getEmail(),
                utilisateur.getNom(),
                utilisateur.getRole(),
                utilisateur.isActif(),
                utilisateur.getDateCreation());
    }
}
