package com.clara.documentmanagement.service;

import com.clara.documentmanagement.config.MinioProperties;
import com.clara.documentmanagement.dto.response.DocumentResponse;
import com.clara.documentmanagement.dto.response.DownloadUrlResponse;
import com.clara.documentmanagement.dto.response.PagedDocumentResponse;
import com.clara.documentmanagement.exception.DocumentNotFoundException;
import com.clara.documentmanagement.model.Document;
import com.clara.documentmanagement.repository.DocumentRepository;
import com.clara.documentmanagement.repository.DocumentSpecification;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

  private static final String PDF_CONTENT_TYPE = "application/pdf";

  private final DocumentRepository documentRepository;
  private final StorageService storageService;
  private final MinioProperties minioProperties;

  @Transactional
  public DocumentResponse upload(
      String userId, String documentName, List<String> tags, MultipartFile file)
      throws IOException {

    validatePdf(file);

    UUID documentId = UUID.randomUUID();
    String objectKey = buildObjectKey(userId, documentId, documentName);

    // Stream the file to MinIO — no full buffering in heap
    storageService.store(objectKey, file.getInputStream(), file.getSize(), PDF_CONTENT_TYPE);

    Document document =
        Document.builder()
            .id(documentId)
            .userId(userId)
            .documentName(documentName)
            .tags(tags != null ? tags : List.of())
            .minioPath(objectKey)
            .fileSize(file.getSize())
            .fileType(PDF_CONTENT_TYPE)
            .build();

    Document saved = documentRepository.save(document);
    log.info("Uploaded document: id={}, user={}, name={}", saved.getId(), userId, documentName);
    return DocumentResponse.from(saved);
  }

  @Transactional(readOnly = true)
  public PagedDocumentResponse search(
      String userId, String documentName, List<String> tags, int page, int size) {

    Pageable pageable =
        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

    Specification<Document> spec =
        DocumentSpecification.build(userId, documentName, tags);

    Page<DocumentResponse> resultPage =
        documentRepository.findAll(spec, pageable).map(DocumentResponse::from);

    return PagedDocumentResponse.from(resultPage);
  }

  @Transactional(readOnly = true)
  public DownloadUrlResponse generateDownloadUrl(UUID id) {
    Document document =
        documentRepository.findById(id).orElseThrow(() -> new DocumentNotFoundException(id));

    String url = storageService.generatePresignedUrl(document.getMinioPath());

    return DownloadUrlResponse.builder()
        .documentId(id)
        .url(url)
        .expiresInMinutes(minioProperties.getPresignedUrlExpiryMinutes())
        .build();
  }

  private void validatePdf(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("File must not be empty");
    }
    String contentType = file.getContentType();
    if (!PDF_CONTENT_TYPE.equalsIgnoreCase(contentType)) {
      throw new IllegalArgumentException(
          "Only PDF files are accepted. Received content type: " + contentType);
    }
  }

  private String buildObjectKey(String userId, UUID documentId, String documentName) {
    // Structure: {userId}/{documentId}/{documentName}
    // The documentId component guarantees uniqueness even for same-named files.
    return userId + "/" + documentId + "/" + documentName;
  }
}
