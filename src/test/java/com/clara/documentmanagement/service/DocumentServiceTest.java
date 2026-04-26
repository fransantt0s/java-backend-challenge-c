package com.clara.documentmanagement.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.clara.documentmanagement.config.MinioProperties;
import com.clara.documentmanagement.dto.response.DocumentResponse;
import com.clara.documentmanagement.dto.response.DownloadUrlResponse;
import com.clara.documentmanagement.exception.DocumentNotFoundException;
import com.clara.documentmanagement.model.Document;
import com.clara.documentmanagement.repository.DocumentRepository;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

  private static final byte[] PDF_CONTENT =
      "%PDF-1.4 test content".getBytes(StandardCharsets.UTF_8);

  @Mock private DocumentRepository documentRepository;
  @Mock private StorageService storageService;
  @Mock private MinioProperties minioProperties;

  @InjectMocks private DocumentService documentService;

  private MockMultipartFile validPdf;
  private Document savedDocument;

  @BeforeEach
  void setUp() {
    validPdf = new MockMultipartFile("file", "test.pdf", "application/pdf", PDF_CONTENT);

    savedDocument =
        Document.builder()
            .id(UUID.randomUUID())
            .userId("user1")
            .documentName("test.pdf")
            .tags(List.of("tag1", "tag2"))
            .minioPath("user1/some-uuid/test.pdf")
            .fileSize((long) PDF_CONTENT.length)
            .fileType("application/pdf")
            .createdAt(LocalDateTime.now())
            .build();
  }

  // ── Upload ────────────────────────────────────────────────────────────────

  @Test
  void upload_validPdf_storesAndPersists() throws Exception {
    when(documentRepository.save(any(Document.class))).thenReturn(savedDocument);

    DocumentResponse response =
        documentService.upload("user1", "test.pdf", List.of("tag1", "tag2"), validPdf);

    verify(storageService)
        .store(anyString(), any(), eq((long) PDF_CONTENT.length), eq("application/pdf"));
    verify(documentRepository).save(any(Document.class));

    assertThat(response.getUserId()).isEqualTo("user1");
    assertThat(response.getDocumentName()).isEqualTo("test.pdf");
    assertThat(response.getTags()).containsExactlyInAnyOrder("tag1", "tag2");
  }

  @Test
  void upload_objectKeyContainsUserId() throws Exception {
    when(documentRepository.save(any(Document.class))).thenReturn(savedDocument);

    documentService.upload("alice", "report.pdf", List.of(), validPdf);

    ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
    verify(storageService).store(keyCaptor.capture(), any(), anyLong(), anyString());

    assertThat(keyCaptor.getValue()).startsWith("alice/");
    assertThat(keyCaptor.getValue()).endsWith("/report.pdf");
  }

  @Test
  void upload_nullTags_defaultsToEmptyList() throws Exception {
    ArgumentCaptor<Document> docCaptor = ArgumentCaptor.forClass(Document.class);
    when(documentRepository.save(docCaptor.capture())).thenReturn(savedDocument);

    documentService.upload("user1", "test.pdf", null, validPdf);

    assertThat(docCaptor.getValue().getTags()).isEmpty();
  }

  @Test
  void upload_nonPdfContentType_throwsIllegalArgument() {
    MockMultipartFile nonPdf =
        new MockMultipartFile("file", "image.png", "image/png", new byte[10]);

    assertThatThrownBy(() -> documentService.upload("user1", "image.png", null, nonPdf))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("PDF");
  }

  @Test
  void upload_pdfContentTypeButInvalidMagicNumber_throwsIllegalArgument() {
    MockMultipartFile fakePdf =
        new MockMultipartFile("file", "fake.pdf", "application/pdf", new byte[64]);

    assertThatThrownBy(() -> documentService.upload("user1", "fake.pdf", null, fakePdf))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("%PDF");
  }

  @Test
  void upload_emptyFile_throwsIllegalArgument() {
    MockMultipartFile empty =
        new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);

    assertThatThrownBy(() -> documentService.upload("user1", "empty.pdf", null, empty))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("empty");
  }

  // ── Download ─────────────────────────────────────────────────────────────

  @Test
  void generateDownloadUrl_existingDocument_returnsUrl() {
    when(minioProperties.getPresignedUrlExpiryMinutes()).thenReturn(60);
    when(documentRepository.findById(savedDocument.getId())).thenReturn(Optional.of(savedDocument));
    when(storageService.generatePresignedUrl(savedDocument.getMinioPath()))
        .thenReturn("https://minio/signed-url");

    DownloadUrlResponse response = documentService.generateDownloadUrl(savedDocument.getId());

    assertThat(response.getUrl()).isEqualTo("https://minio/signed-url");
    assertThat(response.getDocumentId()).isEqualTo(savedDocument.getId());
    assertThat(response.getExpiresInMinutes()).isEqualTo(60);
  }

  @Test
  void generateDownloadUrl_missingDocument_throwsNotFound() {
    UUID missingId = UUID.randomUUID();
    when(documentRepository.findById(missingId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> documentService.generateDownloadUrl(missingId))
        .isInstanceOf(DocumentNotFoundException.class)
        .hasMessageContaining(missingId.toString());
  }
}
