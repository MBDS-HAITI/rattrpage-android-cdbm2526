// src/main/java/ht/mbds/calebtoussaint/trailgoapi/web/dto/FichierResponse.java
package ht.mbds.calebtoussaint.trailgoapi.web.dto;

/**
 * Reponse apres upload.
 *
 * @param url        chemin relatif a placer dans imageCouverture ou photo
 * @param urlAbsolue URL complete, directement utilisable dans une balise img
 */
public record FichierResponse(
        String url,
        String urlAbsolue,
        String nomOriginal,
        long tailleOctets
) {}
