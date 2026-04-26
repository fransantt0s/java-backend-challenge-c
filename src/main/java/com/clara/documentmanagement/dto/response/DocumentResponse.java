package com.clara.documentmanagement.dto.response;

import com.clara.documentmanagement.model.Document;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Schema(description = "Metadata of an uploaded document")
public class DocumentResponse {

  @Schema(
      description = "Unique document identifier",
      example = "550e8400-e29b-41d4-a716-446655440000")
  UUID id;

  @Schema(description = "User who uploaded the document", example = "alice")
  String userId;

  @Schema(description = "Name of the document", example = "annual-report.pdf")
  String documentName;

  @Schema(description = "Tags associated with the document", example = "[\"finance\", \"2024\"]")
  List<String> tags;

  @Schema(description = "File size in bytes", example = "1048576")
  Long fileSize;

  @Schema(description = "MIME type", example = "application/pdf")
  String fileType;

  @Schema(description = "Upload timestamp", example = "2024-01-15T10:30:00")
  LocalDateTime createdAt;

  public static DocumentResponse from(Document doc) {
    return DocumentResponse.builder()
        .id(doc.getId())
        .userId(doc.getUserId())
        .documentName(doc.getDocumentName())
        .tags(doc.getTags() != null ? List.copyOf(doc.getTags()) : List.of())
        .fileSize(doc.getFileSize())
        .fileType(doc.getFileType())
        .createdAt(doc.getCreatedAt())
        .build();
  }
}
