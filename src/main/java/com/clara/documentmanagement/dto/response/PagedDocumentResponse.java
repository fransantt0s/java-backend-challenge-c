package com.clara.documentmanagement.dto.response;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.domain.Page;

@Value
@Builder
public class PagedDocumentResponse {

  List<DocumentResponse> content;
  int page;
  int size;
  long totalElements;
  int totalPages;
  boolean last;

  public static PagedDocumentResponse from(Page<DocumentResponse> page) {
    return PagedDocumentResponse.builder()
        .content(page.getContent())
        .page(page.getNumber())
        .size(page.getSize())
        .totalElements(page.getTotalElements())
        .totalPages(page.getTotalPages())
        .last(page.isLast())
        .build();
  }
}
