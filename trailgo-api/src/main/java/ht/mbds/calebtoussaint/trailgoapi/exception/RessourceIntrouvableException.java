// src/main/java/ht/mbds/calebtoussaint/trailgoapi/exception/RessourceIntrouvableException.java
package ht.mbds.calebtoussaint.trailgoapi.exception;

/** Ressource demandee inexistante. Traduite en HTTP 404. */
public class RessourceIntrouvableException extends RuntimeException {

    public RessourceIntrouvableException(String ressource, Object id) {
        super("%s introuvable pour l'identifiant %s".formatted(ressource, id));
    }
}
