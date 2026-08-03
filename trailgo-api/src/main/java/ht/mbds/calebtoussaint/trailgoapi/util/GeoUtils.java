// src/main/java/ht/mbds/calebtoussaint/trailgoapi/util/GeoUtils.java
package ht.mbds.calebtoussaint.trailgoapi.util;

import org.locationtech.jts.geom.*;

import java.util.List;

/**
 * Fabrique centralisee de geometries.
 *
 * POURQUOI CETTE CLASSE :
 * Si on cree une geometrie sans preciser le SRID, PostgreSQL la rejette
 * avec "Geometry SRID (0) does not match column SRID (4326)".
 * En passant toujours par ici, le SRID est garanti.
 *
 * PIEGE A RETENIR : dans JTS et PostGIS, X = LONGITUDE et Y = LATITUDE.
 * C'est l'inverse de l'habitude (on dit "latitude, longitude"). Les
 * methodes ci-dessous prennent la latitude en premier, comme un humain,
 * et font l'inversion en interne.
 */
public final class GeoUtils {

    /** WGS84 : le systeme de coordonnees du GPS. */
    public static final int SRID = 4326;

    private static final GeometryFactory FACTORY =
            new GeometryFactory(new PrecisionModel(), SRID);

    private GeoUtils() {
        // Classe utilitaire : pas d'instanciation.
    }

    public static GeometryFactory factory() {
        return FACTORY;
    }

    /** Cree un point a partir d'une latitude et d'une longitude. */
    public static Point point(double latitude, double longitude) {
        return FACTORY.createPoint(new Coordinate(longitude, latitude));
    }

    /** Cree une ligne (le trace d'un parcours) a partir de points ordonnes. */
    public static LineString lineString(List<Coordinate> coordonnees) {
        if (coordonnees == null || coordonnees.size() < 2) {
            throw new IllegalArgumentException("Un trace doit comporter au moins 2 points");
        }
        return FACTORY.createLineString(coordonnees.toArray(new Coordinate[0]));
    }

    /**
     * Rectangle englobant une geometrie (utilise pour parcours.bbox).
     * Renvoie null si la geometrie est vide ou reduite a un point.
     */
    public static Polygon boundingBox(Geometry geometrie) {
        if (geometrie == null || geometrie.isEmpty()) {
            return null;
        }
        Geometry enveloppe = geometrie.getEnvelope();
        if (!(enveloppe instanceof Polygon polygone)) {
            return null;
        }
        polygone.setSRID(SRID);
        return polygone;
    }

    /**
     * Distance en metres entre deux points GPS (formule de Haversine).
     * Cette meme formule sera reimplementee a l'identique dans l'app
     * Android pour fonctionner hors ligne.
     */
    public static double distanceMetres(double lat1, double lng1,
                                        double lat2, double lng2) {
        final double rayonTerreM = 6_371_000d;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * rayonTerreM * Math.asin(Math.sqrt(a));
    }
}
