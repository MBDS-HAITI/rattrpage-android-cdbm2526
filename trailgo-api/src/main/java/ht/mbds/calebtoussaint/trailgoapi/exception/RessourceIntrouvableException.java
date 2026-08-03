// src/main/java/ht/mbds/calebtoussaint/trailgoapi/exception/RessourceIntrouvableException.java
package ht.mbds.calebtoussaint.trailgoapi.exception;

/** Ressource demandee inexistante. Traduite en HTTP 404. */
public class RessourceIntrouvableException extends RuntimeException {

    /** Forme courante : ("Parcours", 42) -> "Parcours introuvable pour l'identifiant 42". */
    public RessourceIntrouvableException(String ressource, Object id) {
        super("%s introuvable pour l'identifiant %s".formatted(ressource, id));
    }

    /** Forme libre, pour les messages qui ne suivent pas ce schema. */
    public RessourceIntrouvableException(String message) {
        super(message);
    }
}