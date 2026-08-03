// src/main/java/ht/mbds/calebtoussaint/trailgoapi/exception/RegleMetierException.java
package ht.mbds.calebtoussaint.trailgoapi.exception;

/**
 * Violation d'une regle metier : publier un parcours vide, noter deux
 * fois le meme parcours... Traduite en HTTP 409 (Conflict).
 */
public class RegleMetierException extends RuntimeException {

    public RegleMetierException(String message) {
        super(message);
    }
}
