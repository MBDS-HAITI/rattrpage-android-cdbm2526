// src/main/java/ht/mbds/calebtoussaint/trailgoapi/config/WebConfig.java
package ht.mbds.calebtoussaint.trailgoapi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Expose le dossier d'upload comme ressource statique.
 *
 * Sans cette configuration, les images enregistrees existeraient sur le
 * disque mais ne seraient accessibles par aucune URL : le back office
 * React et l'application Android ne pourraient pas les afficher.
 *
 * Apres cela, /uploads/mon-image.jpg sert directement le fichier
 * ./uploads/mon-image.jpg du disque.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String dossierUpload;

    public WebConfig(@Value("${trailgo.storage.upload-dir}") String dossierUpload) {
        this.dossierUpload = dossierUpload;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path chemin = Paths.get(dossierUpload).toAbsolutePath().normalize();

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + chemin + "/");
    }
}
