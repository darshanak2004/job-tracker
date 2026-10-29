package com.jobtracker.dto;

import java.util.List;
import org.springframework.data.domain.Page;

/** Stable JSON shape for paged results (we don't expose Spring's Page directly). */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResponse<T> from(Page<T> p) {
        return new PageResponse<>(p.getContent(), p.getNumber(), p.getSize(),
                p.getTotalElements(), p.getTotalPages());
    }
}
