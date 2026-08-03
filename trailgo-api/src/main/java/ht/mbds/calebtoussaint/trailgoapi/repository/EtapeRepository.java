// src/main/java/ht/mbds/calebtoussaint/trailgoapi/repository/EtapeRepository.java
package ht.mbds.calebtoussaint.trailgoapi.repository;

import ht.mbds.calebtoussaint.trailgoapi.domain.Etape;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EtapeRepository extends JpaRepository<Etape, Long> {

    /**
     * Nom de methode "magique" : Spring Data lit le nom et genere la
     * requete. findBy + ParcoursId + OrderBy + Ordre + Asc.
     * Aucune requete a ecrire.
     */
    List<Etape> findByParcoursIdOrderByOrdreAsc(Long parcoursId);

    /** Sert a placer une nouvelle etape en fin de parcours. */
    @Query("SELECT COALESCE(MAX(e.ordre), 0) FROM Etape e WHERE e.parcours.id = :parcoursId")
    Integer trouverOrdreMax(@Param("parcoursId") Long parcoursId);
}
