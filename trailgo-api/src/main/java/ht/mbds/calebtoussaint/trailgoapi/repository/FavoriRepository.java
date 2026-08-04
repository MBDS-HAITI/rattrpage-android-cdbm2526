// src/main/java/ht/mbds/calebtoussaint/trailgoapi/repository/FavoriRepository.java
package ht.mbds.calebtoussaint.trailgoapi.repository;

import ht.mbds.calebtoussaint.trailgoapi.domain.Favori;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FavoriRepository extends JpaRepository<Favori, Favori.FavoriId> {

    /**
     * Favoris d'un utilisateur, avec le parcours charge en une requete.
     * L'application mobile appelle cet endpoint au demarrage.
     */
    @Query(value = """
           SELECT f FROM Favori f
           JOIN FETCH f.parcours
           WHERE f.utilisateur.id = :utilisateurId
           ORDER BY f.dateAjout DESC
           """,
           countQuery = "SELECT COUNT(f) FROM Favori f WHERE f.utilisateur.id = :utilisateurId")
    Page<Favori> findByUtilisateurId(@Param("utilisateurId") Long utilisateurId,
                                     Pageable pageable);

    boolean existsByUtilisateurIdAndParcoursId(Long utilisateurId, Long parcoursId);

    void deleteByUtilisateurIdAndParcoursId(Long utilisateurId, Long parcoursId);

    long countByParcoursId(Long parcoursId);
}
