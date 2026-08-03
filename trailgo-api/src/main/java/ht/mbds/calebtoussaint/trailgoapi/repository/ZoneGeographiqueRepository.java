// src/main/java/ht/mbds/calebtoussaint/trailgoapi/repository/ZoneGeographiqueRepository.java
package ht.mbds.calebtoussaint.trailgoapi.repository;

import ht.mbds.calebtoussaint.trailgoapi.domain.Parcours;
import ht.mbds.calebtoussaint.trailgoapi.domain.ZoneGeographique;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ZoneGeographiqueRepository extends JpaRepository<ZoneGeographique, Long> {

    long countByNomIgnoreCase(String nom);

    /**
     * Superficie de la zone en km2.
     *
     * Meme logique que pour les longueurs : ST_Area sur une "geometry"
     * en SRID 4326 renvoie des degres carres, sans signification.
     * Le cast ::geography donne des metres carres ; on divise par un
     * million pour obtenir des km2.
     */
    @Query(value = """
            SELECT ST_Area(z.polygone::geography) / 1000000.0
            FROM zone_geographique z
            WHERE z.id = :id
            """, nativeQuery = true)
    Double calculerSuperficieKm2(@Param("id") Long id);

    /**
     * Parcours geographiquement inclus dans la zone.
     *
     * ST_Within(A, B) est vrai quand A est ENTIEREMENT contenu dans B.
     * A distinguer de ST_Intersects, qui suffit d'un simple contact :
     * un parcours qui ne fait que traverser la zone en ressort ici
     * exclu, ce qui est le comportement voulu pour un rattachement.
     */
    @Query(value = """
            SELECT p.* FROM parcours p
            JOIN zone_geographique z ON z.id = :zoneId
            WHERE p.trace IS NOT NULL
              AND ST_Within(p.trace, z.polygone)
            ORDER BY p.titre
            """, nativeQuery = true)
    List<Parcours> parcoursInclus(@Param("zoneId") Long zoneId);

    /**
     * Rattachement automatique d'un parcours a sa zone.
     *
     * ORDER BY ST_Area ASC : quand plusieurs zones imbriquees contiennent
     * le parcours (le quartier dans la ville, la ville dans le
     * departement), on retient LA PLUS PETITE, donc la plus precise.
     *
     * Si aucune zone ne convient, la sous-requete renvoie NULL et le
     * parcours se retrouve sans zone : c'est le comportement souhaite.
     */
    @Modifying
    @Query(value = """
            UPDATE parcours p
            SET zone_id = (
                SELECT z.id FROM zone_geographique z
                WHERE ST_Within(p.trace, z.polygone)
                ORDER BY ST_Area(z.polygone) ASC
                LIMIT 1)
            WHERE p.id = :parcoursId AND p.trace IS NOT NULL
            """, nativeQuery = true)
    int rattacherAutomatiquement(@Param("parcoursId") Long parcoursId);

    /** Meme operation pour tous les parcours ayant un trace. */
    @Modifying
    @Query(value = """
            UPDATE parcours p
            SET zone_id = (
                SELECT z.id FROM zone_geographique z
                WHERE ST_Within(p.trace, z.polygone)
                ORDER BY ST_Area(z.polygone) ASC
                LIMIT 1)
            WHERE p.trace IS NOT NULL
            """, nativeQuery = true)
    int rattacherTousLesParcours();

    long countByIdIn(List<Long> ids);
}
