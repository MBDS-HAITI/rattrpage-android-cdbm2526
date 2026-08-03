// src/main/java/ht/mbds/calebtoussaint/trailgoapi/util/GpxParser.java
package ht.mbds.calebtoussaint.trailgoapi.util;

import ht.mbds.calebtoussaint.trailgoapi.exception.RegleMetierException;
import org.locationtech.jts.geom.Coordinate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Lecture d'un fichier GPX.
 *
 * Le GPX est un format XML. Structure typique :
 *
 *   <gpx>
 *     <trk>
 *       <trkseg>
 *         <trkpt lat="18.5479" lon="-72.3395"><ele>25</ele></trkpt>
 *         <trkpt lat="18.5426" lon="-72.3378"/>
 *       </trkseg>
 *     </trk>
 *   </gpx>
 *
 * On lit les <trkpt> (points de trace). A defaut, on se rabat sur les
 * <rtept> (points de route), utilises par certains outils d'itineraire.
 *
 * SECURITE : le parseur XML est configure pour refuser les DTD externes.
 * Sans cela, un fichier GPX malveillant pourrait declencher une attaque
 * XXE (XML External Entity) et faire lire des fichiers du serveur.
 */
public final class GpxParser {

    private GpxParser() {
        // classe utilitaire
    }

    /**
     * Extrait les coordonnees d'un flux GPX.
     * @return liste de Coordinate au format JTS, soit (x = lon, y = lat)
     */
    public static List<Coordinate> lireCoordonnees(InputStream flux) {
        try {
            DocumentBuilderFactory fabrique = DocumentBuilderFactory.newInstance();

            // --- Protection contre les attaques XXE ---
            fabrique.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            fabrique.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            fabrique.setXIncludeAware(false);
            fabrique.setExpandEntityReferences(false);

            DocumentBuilder constructeur = fabrique.newDocumentBuilder();
            Document document = constructeur.parse(flux);
            document.getDocumentElement().normalize();

            List<Coordinate> coordonnees = extraire(document, "trkpt");
            if (coordonnees.isEmpty()) {
                coordonnees = extraire(document, "rtept");
            }
            if (coordonnees.size() < 2) {
                throw new RegleMetierException(
                        "Le fichier GPX ne contient pas assez de points "
                        + "(2 minimum, " + coordonnees.size() + " trouve(s))");
            }
            return coordonnees;

        } catch (RegleMetierException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RegleMetierException(
                    "Fichier GPX illisible ou malforme : " + ex.getMessage());
        }
    }

    private static List<Coordinate> extraire(Document document, String nomBalise) {
        List<Coordinate> coordonnees = new ArrayList<>();
        NodeList noeuds = document.getElementsByTagName(nomBalise);

        for (int i = 0; i < noeuds.getLength(); i++) {
            Element point = (Element) noeuds.item(i);
            String lat = point.getAttribute("lat");
            String lon = point.getAttribute("lon");

            if (lat.isBlank() || lon.isBlank()) {
                continue;
            }
            try {
                double latitude = Double.parseDouble(lat);
                double longitude = Double.parseDouble(lon);

                if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
                    continue;   // point aberrant : on l'ignore silencieusement
                }
                // JTS : x = longitude, y = latitude
                coordonnees.add(new Coordinate(longitude, latitude));

            } catch (NumberFormatException ignore) {
                // attribut non numerique : point ignore
            }
        }
        return coordonnees;
    }
}
