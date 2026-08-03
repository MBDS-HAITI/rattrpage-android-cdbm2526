// src/main/java/ht/mbds/calebtoussaint/trailgoapi/exception/AuthentificationException.java
package ht.mbds.calebtoussaint.trailgoapi.exception;

/** Identifiants incorrects ou compte desactive. Traduite en HTTP 401. */
public class AuthentificationException extends RuntimeException {

    public AuthentificationException(String message) {
        super(message);
    }
}
