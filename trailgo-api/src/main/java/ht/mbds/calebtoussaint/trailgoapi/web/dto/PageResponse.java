// src/main/java/ht/mbds/calebtoussaint/trailgoapi/web/dto/PageResponse.java
package ht.mbds.calebtoussaint.trailgoapi.web.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Enveloppe de pagination.
 *
 * POURQUOI NE PAS RENVOYER Page<T> DIRECTEMENT : sa structure JSON n'est
 * pas garantie stable entre versions de Spring, et elle expose des champs
 * internes ("pageable", "sort") dont React et Android n'ont aucun usage.
 * Ce contrat-ci est simple, stable et documentable.
 */
public record PageResponse<T>(
        List<T> contenu,
        int page,
        int taille,
        long totalElements,
        int totalPages,
        boolean premiere,
        boolean derniere
) {

    /** Convertit une Page d'entites en PageResponse de DTO. */
    public static <E, D> PageResponse<D> de(Page<E> page, Function<E, D> convertisseur) {
        return new PageResponse<>(
                page.getContent().stream().map(convertisseur).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
