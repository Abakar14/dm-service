package com.bytmasoft.dm.dto.page;

import java.util.List;
import org.springframework.data.domain.Page;

// Common pagination DTOs
public record PagedResponse<T>(List<T> content, PageMetadata metadata) {

  public static <T> PagedResponse<T> of(Page<T> page) {
    return new PagedResponse<>(
        page.getContent(),
        new PageMetadata(
            page.getNumber(),
            page.getSize(),
            page.getTotalElements()
        )
    );
  }
}

