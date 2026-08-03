// src/main/java/ht/mbds/calebtoussaint/trailgoapi/service/StockageFichierService.java
package ht.mbds.calebtoussaint.trailgoapi.service;

import ht.mbds.calebtoussaint.trailgoapi.exception.RegleMetierException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Stockage des images sur le disque local.
 *
 * Le sujet autorise "stockage local ou S3". On reste en local : c'est
 * suffisant pour une demo et cela evite une dependance externe.
 *
 * TROIS REGLES DE SECURITE APPLIQUEES ICI :
 *
 * 1. Le nom de fichier envoye par le client n'est JAMAIS reutilise.
 *    Un nom comme "../../../etc/passwd" permettrait d'ecrire n'importe
 *    ou sur le disque (attaque dite "path traversal"). On genere donc
 *    un UUID aleatoire.
 *
 * 2. Seules quelques extensions d'images sont acceptees, et le type MIME
 *    declare est verifie.
 *
 * 3. La taille maximale est plafonnee dans application.yaml (10 Mo).
 */
@Service
public class StockageFichierService {

    private static final Logger log = LoggerFactory.getLogger(StockageFichierService.class);

    private static final List<String> EXTENSIONS_AUTORISEES =
            List.of("jpg", "jpeg", "png", "webp", "gif");

    private static final List<String> TYPES_MIME_AUTORISES =
            List.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private final Path dossierRacine;

    public StockageFichierService(@Value("${trailgo.storage.upload-dir}") String dossier) {
        this.dossierRacine = Paths.get(dossier).toAbsolutePath().normalize();
    }

    /** Cree le dossier de stockage au demarrage s'il n'existe pas. */
    @PostConstruct
    public void initialiser() {
        try {
            Files.createDirectories(dossierRacine);
            log.info("Dossier de stockage des images : {}", dossierRacine);
        } catch (IOException ex) {
            throw new IllegalStateException(
                    "Impossible de creer le dossier de stockage " + dossierRacine, ex);
        }
    }

    /**
     * Enregistre une image et renvoie le chemin public d'acces.
     *
     * @return une URL relative du type /uploads/3f7a...c2.jpg
     */
    public String enregistrerImage(MultipartFile fichier) {
        if (fichier == null || fichier.isEmpty()) {
            throw new RegleMetierException("Le fichier est vide");
        }

        String extension = extraireExtension(fichier.getOriginalFilename());

        if (!EXTENSIONS_AUTORISEES.contains(extension)) {
            throw new RegleMetierException(
                    "Extension non autorisee. Formats acceptes : " + EXTENSIONS_AUTORISEES);
        }
        if (fichier.getContentType() == null
                || !TYPES_MIME_AUTORISES.contains(fichier.getContentType().toLowerCase(Locale.ROOT))) {
            throw new RegleMetierException("Le fichier envoye n'est pas une image valide");
        }

        // Nom genere cote serveur : le nom d'origine n'est jamais reutilise.
        String nomStocke = UUID.randomUUID() + "." + extension;
        Path destination = dossierRacine.resolve(nomStocke).normalize();

        // Ceinture et bretelles : on verifie que la destination reste bien
        // dans le dossier prevu, meme si l'UUID etait detourne.
        if (!destination.getParent().equals(dossierRacine)) {
            throw new RegleMetierException("Chemin de destination invalide");
        }

        try {
            Files.copy(fichier.getInputStream(), destination,
                       StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            log.error("Echec de l'enregistrement du fichier {}", nomStocke, ex);
            throw new RegleMetierException("Impossible d'enregistrer le fichier");
        }

        return "/uploads/" + nomStocke;
    }

    /**
     * Supprime une image a partir de son URL publique.
     * Ne leve pas d'erreur si le fichier n'existe plus.
     */
    public void supprimerImage(String urlPublique) {
        if (urlPublique == null || !urlPublique.startsWith("/uploads/")) {
            return;
        }
        String nom = urlPublique.substring("/uploads/".length());
        Path chemin = dossierRacine.resolve(nom).normalize();

        if (!chemin.getParent().equals(dossierRacine)) {
            return;   // tentative de sortie du dossier : on ignore
        }
        try {
            Files.deleteIfExists(chemin);
        } catch (IOException ex) {
            log.warn("Impossible de supprimer le fichier {}", nom, ex);
        }
    }

    private String extraireExtension(String nomOriginal) {
        if (nomOriginal == null) {
            return "";
        }
        int point = nomOriginal.lastIndexOf('.');
        if (point < 0 || point == nomOriginal.length() - 1) {
            return "";
        }
        return nomOriginal.substring(point + 1).toLowerCase(Locale.ROOT);
    }
}
