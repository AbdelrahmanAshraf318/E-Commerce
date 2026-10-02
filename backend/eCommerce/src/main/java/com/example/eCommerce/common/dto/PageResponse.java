package com.example.eCommerce.common.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Stable JSON shape for paged results. Serializing Spring's PageImpl directly is discouraged
 * (its JSON structure is not guaranteed between versions).
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages)
{
    public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper)
    {
        return new PageResponse<>(page.getContent().stream().map(mapper).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
