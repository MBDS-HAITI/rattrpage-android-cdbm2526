// src/main/java/ht/mbds/calebtoussaint/trailgoapi/repository/RechercheSpatialeRepository.java
package ht.mbds.calebtoussaint.trailgoapi.repository;

import ht.mbds.calebtoussaint.trailgoapi.domain.Parcours;
import ht.mbds.calebtoussaint.trailgoapi.repository.projection.EtapeProche;
import ht.mbds.calebtoussaint.trailgoapi.repository.projection.ParcoursProche;
import ht.mbds.calebtoussaint.trailgoapi.repository.projection.PoiProche;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Requetes spatiales PostGIS.
 *
 * Toutes sont natives : les fonctions ST_* et le cast ::geography
 * n'existent pas en JPQL.
 *
 * =====================================================================
 * LES DEUX POINTS A COMPRENDRE
 * =====================================================================
 *
 * 1. LE CAST ::geography
 *
 *    En SRID 4326 les coordonnees sont des DEGRES d'angle.
 *      ST_DWithin(position, point, 200)
 *    cherche donc dans un rayon de 200 DEGRES, soit toute la planete.
 *
 *      ST_DWithin(position::geography, point::geography, 200)
 *    fait un calcul geodesique sur l'ellipsoide WGS84 et interprete
 *    bien 200 comme 200 METRES.
 *
 * 2. ST_DWithin PLUTOT QUE ST_Distance(...) < X
 *
 *    Les deux donnent le meme resultat, mais seul ST_DWithin sait
 *    utiliser l'index GIST cree dans la migration V1. Avec ST_Distance,
 *    PostgreSQL calcule la distance pour CHAQUE ligne de la table avant
 *    de filtrer : sur 10 000 parcours la difference est enorme.
 *
 *    C'est une question classique en soutenance.
 * =====================================================================
 *
 * Cette interface etend Repository (et non JpaRepository) : elle ne gere
 * aucune entite en propre, elle ne fait que porter des requetes.
 */
public interface RechercheSpatialeRepository extends Repository<Parcours, Long> {

    /**
     * Parcours publies dont AU MOINS UNE etape se trouve dans le rayon.
     *
     * GROUP BY + MIN : un parcours peut avoir plusieurs etapes dans la
     * zone. On ne le renvoie qu'une fois, avec la distance de son etape
     * la plus proche, et on trie du plus proche au plus lointain.
     *
     * ST_SetSRID(ST_MakePoint(lng, lat), 4326) construit le point de
     * reference. Attention a l'ordre : ST_MakePoint prend (X, Y),
     * c'est-a-dire (longitude, latitude).
     */
    @Query(value = """
            SELECT p.id                AS id,
                   p.titre             AS titre,
                   p.theme             AS theme,
                   p.difficulte        AS difficulte,
                   p.duree_estimee_min AS duree_estimee_min,
                   p.image_couverture  AS image_couverture,
                   p.distance_totale_km AS distance_totale_km,
                   MIN(ST_Distance(
                       e.position::geography,
                       ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography
                   ))                  AS distance_m
            FROM parcours p
            JOIN etape e ON e.parcours_id = p.id
            WHERE p.statut = 'PUBLIE'
              AND ST_DWithin(
                      e.position::geography,
                      ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,
                      :rayonM)
            GROUP BY p.id, p.titre, p.theme, p.difficulte,
                     p.duree_estimee_min, p.image_couverture, p.distance_totale_km
            ORDER BY distance_m ASC
            LIMIT :limite
            """, nativeQuery = true)
    List<ParcoursProche> parcoursDansRayon(@Param("lat") double latitude,
                                           @Param("lng") double longitude,
                                           @Param("rayonM") double rayonMetres,
                                           @Param("limite") int limite);

    /**
     * Parcours dont le trace traverse le rectangle affiche a l'ecran.
     *
     * ST_MakeEnvelope(ouest, sud, est, nord, 4326) construit le
     * rectangle. C'est exactement ce que Leaflet fournit via
     * map.getBounds() quand l'utilisateur deplace la carte.
     *
     * ST_Intersects est vrai des que les deux geometries se touchent,
     * meme partiellement : un parcours qui ne fait que traverser l'ecran
     * est renvoye, ce qui est le comportement attendu.
     */
    @Query(value = """
            SELECT p.id                AS id,
                   p.titre             AS titre,
                   p.theme             AS theme,
                   p.difficulte        AS difficulte,
                   p.duree_estimee_min AS duree_estimee_min,
                   p.image_couverture  AS image_couverture,
                   p.distance_totale_km AS distance_totale_km,
                   NULL::double precision AS distance_m
            FROM parcours p
            WHERE p.statut = 'PUBLIE'
              AND p.trace IS NOT NULL
              AND ST_Intersects(
                      p.trace,
                      ST_MakeEnvelope(:ouest, :sud, :est, :nord, 4326))
            ORDER BY p.titre
            LIMIT :limite
            """, nativeQuery = true)
    List<ParcoursProche> parcoursDansRectangle(@Param("ouest") double ouest,
                                               @Param("sud") double sud,
                                               @Param("est") double est,
                                               @Param("nord") double nord,
                                               @Param("limite") int limite);

    /**
     * Etapes a proximite immediate.
     *
     * Alimente la detection d'arrivee a une etape dans l'application
     * Android : on interroge avec la position GPS courante et un rayon
     * de quelques dizaines de metres.
     *
     * ST_Y(position) = latitude, ST_X(position) = longitude.
     */
    @Query(value = """
            SELECT e.id            AS id,
                   e.nom           AS nom,
                   e.description   AS description,
                   ST_Y(e.position) AS latitude,
                   ST_X(e.position) AS longitude,
                   e.ordre         AS ordre,
                   e.photo         AS photo,
                   p.id            AS parcours_id,
                   p.titre         AS parcours_titre,
                   ST_Distance(
                       e.position::geography,
                       ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography
                   )               AS distance_m
            FROM etape e
            JOIN parcours p ON p.id = e.parcours_id
            WHERE ST_DWithin(
                      e.position::geography,
                      ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,
                      :rayonM)
            ORDER BY distance_m ASC
            LIMIT :limite
            """, nativeQuery = true)
    List<EtapeProche> etapesDansRayon(@Param("lat") double latitude,
                                      @Param("lng") double longitude,
                                      @Param("rayonM") double rayonMetres,
                                      @Param("limite") int limite);

    /**
     * Points d'interet a proximite.
     *
     * Alimente l'alerte "monument a moins de 200 m" de l'application
     * Android. Le rayon par defaut de chaque POI est stocke en base
     * (colonne rayon_proximite_m) mais peut etre surcharge par l'appel.
     */
    @Query(value = """
            SELECT poi.id                AS id,
                   poi.titre             AS titre,
                   poi.categorie         AS categorie,
                   poi.adresse           AS adresse,
                   ST_Y(poi.position)    AS latitude,
                   ST_X(poi.position)    AS longitude,
                   poi.rayon_proximite_m AS rayon_proximite_m,
                   ST_Distance(
                       poi.position::geography,
                       ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography
                   )                     AS distance_m
            FROM point_interet poi
            WHERE ST_DWithin(
                      poi.position::geography,
                      ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,
                      :rayonM)
            ORDER BY distance_m ASC
            LIMIT :limite
            """, nativeQuery = true)
    List<PoiProche> poisDansRayon(@Param("lat") double latitude,
                                  @Param("lng") double longitude,
                                  @Param("rayonM") double rayonMetres,
                                  @Param("limite") int limite);
}
