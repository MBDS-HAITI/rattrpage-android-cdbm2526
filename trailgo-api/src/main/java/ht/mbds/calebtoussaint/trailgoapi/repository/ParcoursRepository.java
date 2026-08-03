// src/main/java/ht/mbds/calebtoussaint/trailgoapi/repository/ParcoursRepository.java
package ht.mbds.calebtoussaint.trailgoapi.repository;

import ht.mbds.calebtoussaint.trailgoapi.domain.Parcours;
import ht.mbds.calebtoussaint.trailgoapi.domain.enums.Difficulte;
import ht.mbds.calebtoussaint.trailgoapi.domain.enums.StatutParcours;
import ht.mbds.calebtoussaint.trailgoapi.domain.enums.Theme;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ParcoursRepository extends JpaRepository<Parcours, Long> {

    /**
     * Recherche avec filtres optionnels.
     *
     * ATTENTION AU PARAMETRE "recherche" :
     * il ne doit JAMAIS valoir null. Quand un parametre String vaut null,
     * le pilote PostgreSQL ne peut pas deviner son type et l'envoie comme
     * "bytea" (binaire). PostgreSQL cherche alors LOWER(bytea), qui
     * n'existe pas, et rejette la requete.
     *
     * Le service passe donc "%" (le joker SQL) quand aucune recherche
     * n'est demandee : la condition est alors toujours vraie.
     *
     * COALESCE sur la description : elle peut etre null en base, et
     * "null LIKE '%'" vaut null (donc faux) et non vrai.
     */
    @Query("""
           SELECT p FROM Parcours p
           WHERE (:theme      IS NULL OR p.theme = :theme)
             AND (:difficulte IS NULL OR p.difficulte = :difficulte)
             AND (:statut     IS NULL OR p.statut = :statut)
             AND (:dureeMax   IS NULL OR p.dureeEstimeeMin <= :dureeMax)
             AND (LOWER(p.titre) LIKE :recherche
                  OR LOWER(COALESCE(p.description, '')) LIKE :recherche)
           """)
    Page<Parcours> rechercher(@Param("theme") Theme theme,
                              @Param("difficulte") Difficulte difficulte,
                              @Param("statut") StatutParcours statut,
                              @Param("dureeMax") Integer dureeMax,
                              @Param("recherche") String recherche,
                              Pageable pageable);

    /**
     * Charge un parcours ET ses etapes en UNE seule requete SQL.
     * Sans le JOIN FETCH, Hibernate ferait une requete pour le parcours
     * puis une autre pour les etapes (probleme dit "N+1").
     */
    @Query("SELECT p FROM Parcours p LEFT JOIN FETCH p.etapes WHERE p.id = :id")
    Optional<Parcours> findByIdAvecEtapes(@Param("id") Long id);

    long countByStatut(StatutParcours statut);

    /** Incrementation directe en SQL : evite de charger l'entite entiere. */
    @Modifying
    @Query("UPDATE Parcours p SET p.nbConsultations = p.nbConsultations + 1 WHERE p.id = :id")
    void incrementerConsultations(@Param("id") Long id);
}