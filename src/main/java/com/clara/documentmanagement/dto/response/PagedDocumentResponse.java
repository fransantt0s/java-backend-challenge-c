package com.clara.documentmanagement.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.domain.Page;

@Value
@Builder
@Schema(description = "Paginated list of document metadata")
public class PagedDocumentResponse {

  @Schema(description = "Documents on the current page")
  List<DocumentResponse> content;

  @Schema(description = "Current page number (zero-based)", example = "0")
  int page;

  @Schema(description = "Number of items per page", example = "10")
  int size;

  @Schema(description = "Total number of documents matching the filter", example = "42")
  long totalElements;

  @Schema(description = "Total number of pages", example = "5")
  int totalPages;

  @Schema(description = "Whether this is the last page", example = "false")
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
