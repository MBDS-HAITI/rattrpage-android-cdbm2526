// src/main/java/ht/mbds/calebtoussaint/trailgoapi/repository/AvisRepository.java
package ht.mbds.calebtoussaint.trailgoapi.repository;

import ht.mbds.calebtoussaint.trailgoapi.domain.Avis;
import ht.mbds.calebtoussaint.trailgoapi.repository.projection.StatistiqueAvis;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AvisRepository extends JpaRepository<Avis, Long> {

    /**
     * JOIN FETCH sur l'auteur : sans cela, afficher 20 avis avec le nom
     * de leur auteur declencherait 21 requetes (probleme N+1).
     */
    @Query(value = """
           SELECT a FROM Avis a
           JOIN FETCH a.auteur
           WHERE a.parcours.id = :parcoursId
           """,
           countQuery = "SELECT COUNT(a) FROM Avis a WHERE a.parcours.id = :parcoursId")
    Page<Avis> findByParcoursId(@Param("parcoursId") Long parcoursId, Pageable pageable);

    /** Avis signales, pour la moderation du back office. */
    @Query(value = """
           SELECT a FROM Avis a
           JOIN FETCH a.auteur
           JOIN FETCH a.parcours
           WHERE a.signale = true
           """,
           countQuery = "SELECT COUNT(a) FROM Avis a WHERE a.signale = true")
    Page<Avis> findSignales(Pageable pageable);

    boolean existsByParcoursIdAndAuteurId(Long parcoursId, Long auteurId);

    Optional<Avis> findByParcoursIdAndAuteurId(Long parcoursId, Long auteurId);

    /**
     * Agregation EN LOT des notes pour plusieurs parcours.
     *
     * Sans cette methode, afficher la note moyenne sur une liste de
     * 20 parcours demanderait 21 requetes. Ici : une seule.
     */
    @Query("""
           SELECT a.parcours.id AS parcoursId,
                  AVG(a.note)   AS moyenne,
                  COUNT(a)      AS total
           FROM Avis a
           WHERE a.parcours.id IN :parcoursIds
           GROUP BY a.parcours.id
           """)
    List<StatistiqueAvis> agregerParParcours(@Param("parcoursIds") Collection<Long> parcoursIds);

    /** Repartition des notes de 1 a 5, pour afficher un histogramme. */
    @Query("""
           SELECT a.note AS note, COUNT(a) AS total
           FROM Avis a
           WHERE a.parcours.id = :parcoursId
           GROUP BY a.note
           ORDER BY a.note
           """)
    List<Object[]> repartitionDesNotes(@Param("parcoursId") Long parcoursId);
}
