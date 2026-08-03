// src/main/java/ht/mbds/calebtoussaint/trailgoapi/exception/GestionnaireExceptions.java
package ht.mbds.calebtoussaint.trailgoapi.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Gestion centralisee des erreurs de toute l'API.
 *
 * @RestControllerAdvice : cette classe intercepte les exceptions levees
 * par n'importe quel controleur. Plus besoin de try/catch partout.
 *
 * Le format de reponse est ProblemDetail (norme RFC 7807), le standard
 * pour les erreurs d'API REST. Exemple de sortie :
 * {
 *   "type": "https://trailgo.local/erreurs/validation",
 *   "title": "Donnees invalides",
 *   "status": 400,
 *   "detail": "La requete contient 2 champ(s) invalide(s)",
 *   "horodatage": "2026-08-02T23:00:00Z",
 *   "erreurs": { "titre": "Le titre est obligatoire" }
 * }
 */
@RestControllerAdvice
public class GestionnaireExceptions {

    private static final Logger log = LoggerFactory.getLogger(GestionnaireExceptions.class);
    private static final String BASE_TYPE = "https://trailgo.local/erreurs/";

    /** 404 */
    @ExceptionHandler(RessourceIntrouvableException.class)
    public ProblemDetail introuvable(RessourceIntrouvableException ex) {
        return construire(HttpStatus.NOT_FOUND, "Ressource introuvable",
                ex.getMessage(), "ressource-introuvable");
    }

    /** 409 */
    @ExceptionHandler(RegleMetierException.class)
    public ProblemDetail regleMetier(RegleMetierException ex) {
        return construire(HttpStatus.CONFLICT, "Conflit metier",
                ex.getMessage(), "regle-metier");
    }

    /** 400 : echec des annotations @NotBlank, @NotNull... sur le corps JSON. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validation(MethodArgumentNotValidException ex) {
        Map<String, String> erreurs = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
          .forEach(f -> erreurs.putIfAbsent(f.getField(), f.getDefaultMessage()));

        ProblemDetail probleme = construire(HttpStatus.BAD_REQUEST, "Donnees invalides",
                "La requete contient %d champ(s) invalide(s)".formatted(erreurs.size()),
                "validation");
        probleme.setProperty("erreurs", erreurs);
        return probleme;
    }

    /** 400 : JSON malforme, ou valeur d'enumeration inconnue. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail corpsIllisible(HttpMessageNotReadableException ex) {
        return construire(HttpStatus.BAD_REQUEST, "Corps de requete illisible",
                "Le JSON est malforme ou contient une valeur non reconnue",
                "corps-illisible");
    }

    /** 400 : par exemple /api/parcours/abc alors qu'un nombre est attendu. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail typeIncorrect(MethodArgumentTypeMismatchException ex) {
        return construire(HttpStatus.BAD_REQUEST, "Parametre de type incorrect",
                "Le parametre '%s' n'a pas le type attendu".formatted(ex.getName()),
                "type-incorrect");
    }

    /**
     * 500 : filet de securite.
     * La stack trace est journalisee cote serveur mais JAMAIS renvoyee au
     * client : ce serait une fuite d'information sur l'architecture interne.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail inattendue(Exception ex) {
        log.error("Erreur inattendue", ex);
        return construire(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur interne",
                "Une erreur inattendue est survenue", "interne");
    }

    private ProblemDetail construire(HttpStatus statut, String titre,
                                     String detail, String type) {
        ProblemDetail probleme = ProblemDetail.forStatusAndDetail(statut, detail);
        probleme.setTitle(titre);
        probleme.setType(URI.create(BASE_TYPE + type));
        probleme.setProperty("horodatage", Instant.now());
        return probleme;
    }
}
