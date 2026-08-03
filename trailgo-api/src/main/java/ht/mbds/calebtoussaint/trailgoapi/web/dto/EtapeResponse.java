// src/main/java/ht/mbds/calebtoussaint/trailgoapi/web/dto/EtapeResponse.java
package ht.mbds.calebtoussaint.trailgoapi.web.dto;

/** Etape telle qu'elle est renvoyee au client, en JSON. */
public record EtapeResponse(
        Long id,
        String nom,
        String description,
        Double latitude,
        Double longitude,
        Integer ordre,
        String photo,
        Integer dureeVisiteMin
) {}
