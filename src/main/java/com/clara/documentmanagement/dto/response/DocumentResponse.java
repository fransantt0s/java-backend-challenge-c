package com.clara.documentmanagement.dto.response;

import com.clara.documentmanagement.model.Document;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DocumentResponse {

  UUID id;
  String userId;
  String documentName;
  List<String> tags;
  String minioPath;
  Long fileSize;
  String fileType;
  LocalDateTime createdAt;

  public static DocumentResponse from(Document doc) {
    return DocumentResponse.builder()
        .id(doc.getId())
        .userId(doc.getUserId())
        .documentName(doc.getDocumentName())
        .tags(doc.getTags())
        .minioPath(doc.getMinioPath())
        .fileSize(doc.getFileSize())
        .fileType(doc.getFileType())
        .createdAt(doc.getCreatedAt())
        .build();
  }
}
