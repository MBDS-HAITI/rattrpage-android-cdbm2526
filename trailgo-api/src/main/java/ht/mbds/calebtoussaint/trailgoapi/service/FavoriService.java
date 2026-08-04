// src/main/java/ht/mbds/calebtoussaint/trailgoapi/service/FavoriService.java
package ht.mbds.calebtoussaint.trailgoapi.service;

import ht.mbds.calebtoussaint.trailgoapi.domain.Favori;
import ht.mbds.calebtoussaint.trailgoapi.domain.Parcours;
import ht.mbds.calebtoussaint.trailgoapi.domain.Utilisateur;
import ht.mbds.calebtoussaint.trailgoapi.exception.RegleMetierException;
import ht.mbds.calebtoussaint.trailgoapi.exception.RessourceIntrouvableException;
import ht.mbds.calebtoussaint.trailgoapi.repository.FavoriRepository;
import ht.mbds.calebtoussaint.trailgoapi.repository.ParcoursRepository;
import ht.mbds.calebtoussaint.trailgoapi.repository.UtilisateurRepository;
import ht.mbds.calebtoussaint.trailgoapi.web.dto.FavoriResponse;
import ht.mbds.calebtoussaint.trailgoapi.web.dto.PageResponse;
import ht.mbds.calebtoussaint.trailgoapi.web.mapper.ParcoursMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FavoriService {

    private final FavoriRepository favoriRepository;
    private final ParcoursRepository parcoursRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ParcoursMapper parcoursMapper;

    public PageResponse<FavoriResponse> listerMesFavoris(String email, Pageable pageable) {
        Utilisateur utilisateur = chargerUtilisateur(email);

        return PageResponse.de(
                favoriRepository.findByUtilisateurId(utilisateur.getId(), pageable),
                favori -> new FavoriResponse(
                        parcoursMapper.versResume(favori.getParcours()),
                        favori.getDateAjout()));
    }

    @Transactional
    public FavoriResponse ajouter(Long parcoursId, String email) {
        Utilisateur utilisateur = chargerUtilisateur(email);
        Parcours parcours = parcoursRepository.findById(parcoursId)
                .orElseThrow(() -> new RessourceIntrouvableException("Parcours", parcoursId));

        if (favoriRepository.existsByUtilisateurIdAndParcoursId(
                utilisateur.getId(), parcoursId)) {
            throw new RegleMetierException("Ce parcours est deja dans vos favoris");
        }

        Favori favori = Favori.builder()
                .id(new Favori.FavoriId(utilisateur.getId(), parcoursId))
                .utilisateur(utilisateur)
                .parcours(parcours)
                .build();

        // saveAndFlush : force l'ecriture immediate en base pour que
        // @CreationTimestamp remplisse dateAjout avant qu'on la lise.
        Favori enregistre = favoriRepository.saveAndFlush(favori);

        return new FavoriResponse(
                parcoursMapper.versResume(parcours),
                enregistre.getDateAjout());
    }

    @Transactional
    public void retirer(Long parcoursId, String email) {
        Utilisateur utilisateur = chargerUtilisateur(email);

        if (!favoriRepository.existsByUtilisateurIdAndParcoursId(
                utilisateur.getId(), parcoursId)) {
            throw new RessourceIntrouvableException(
                    "Ce parcours n'est pas dans vos favoris");
        }
        favoriRepository.deleteByUtilisateurIdAndParcoursId(utilisateur.getId(), parcoursId);
    }

    /** Permet a l'interface d'afficher le coeur plein ou vide. */
    public boolean estEnFavori(Long parcoursId, String email) {
        Utilisateur utilisateur = chargerUtilisateur(email);
        return favoriRepository.existsByUtilisateurIdAndParcoursId(
                utilisateur.getId(), parcoursId);
    }

    private Utilisateur chargerUtilisateur(String email) {
        return utilisateurRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur", email));
    }
}
