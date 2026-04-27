package com.clara.documentmanagement.dto.response;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.clara.documentmanagement.model.Document;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;

class DtoResponseTest {

  private UUID id;
  private LocalDateTime now;
  private DocumentResponse response;

  @BeforeEach
  void setUp() {
    id = UUID.randomUUID();
    now = LocalDateTime.now();
    response =
        DocumentResponse.builder()
            .id(id)
            .userId("user1")
            .documentName("report.pdf")
            .tags(List.of("finance", "2024"))
            .fileSize(1024L)
            .fileType("application/pdf")
            .createdAt(now)
            .build();
  }

  // ── DocumentResponse ──────────────────────────────────────────────────────

  @Test
  void documentResponse_from_mapsAllFields() {
    Document doc =
        Document.builder()
            .id(id)
            .userId("user1")
            .documentName("report.pdf")
            .tags(new java.util.ArrayList<>(List.of("finance")))
            .fileSize(2048L)
            .fileType("application/pdf")
            .minioPath("user1/uuid/report.pdf")
            .createdAt(now)
            .build();

    DocumentResponse result = DocumentResponse.from(doc);

    assertThat(result.getId()).isEqualTo(id);
    assertThat(result.getUserId()).isEqualTo("user1");
    assertThat(result.getDocumentName()).isEqualTo("report.pdf");
    assertThat(result.getTags()).containsExactly("finance");
    assertThat(result.getFileSize()).isEqualTo(2048L);
    assertThat(result.getFileType()).isEqualTo("application/pdf");
    assertThat(result.getCreatedAt()).isEqualTo(now);
  }

  @Test
  void documentResponse_from_nullTags_returnsEmptyList() {
    Document doc =
        Document.builder()
            .id(id)
            .userId("user1")
            .documentName("doc.pdf")
            .fileSize(512L)
            .fileType("application/pdf")
            .minioPath("user1/uuid/doc.pdf")
            .createdAt(now)
            .build();
    doc.setTags(null);

    DocumentResponse result = DocumentResponse.from(doc);

    assertThat(result.getTags()).isEmpty();
  }

  @Test
  void documentResponse_equalsAndHashCode_symmetric() {
    DocumentResponse same =
        DocumentResponse.builder()
            .id(id)
            .userId("user1")
            .documentName("report.pdf")
            .tags(List.of("finance", "2024"))
            .fileSize(1024L)
            .fileType("application/pdf")
            .createdAt(now)
            .build();

    assertThat(response).isEqualTo(same);
    assertThat(response.hashCode()).isEqualTo(same.hashCode());
  }

  @Test
  void documentResponse_notEqualWhenFieldsDiffer() {
    DocumentResponse different =
        DocumentResponse.builder()
            .id(UUID.randomUUID())
            .userId("user2")
            .documentName("other.pdf")
            .tags(List.of())
            .fileSize(999L)
            .fileType("application/pdf")
            .createdAt(now)
            .build();

    assertThat(response).isNotEqualTo(different);
    assertThat(response).isNotEqualTo(null);
    assertThat(response).isNotEqualTo("a string");
  }

  @Test
  void documentResponse_toString_containsFieldValues() {
    String str = response.toString();
    assertThat(str).contains("user1").contains("report.pdf");
  }

  // ── PagedDocumentResponse ─────────────────────────────────────────────────

  @Test
  @SuppressWarnings("unchecked")
  void pagedDocumentResponse_from_mapsPageFields() {
    Page<DocumentResponse> page = mock(Page.class);
    when(page.getContent()).thenReturn(List.of(response));
    when(page.getNumber()).thenReturn(0);
    when(page.getSize()).thenReturn(10);
    when(page.getTotalElements()).thenReturn(1L);
    when(page.getTotalPages()).thenReturn(1);
    when(page.isLast()).thenReturn(true);

    PagedDocumentResponse result = PagedDocumentResponse.from(page);

    assertThat(result.getContent()).containsExactly(response);
    assertThat(result.getPage()).isZero();
    assertThat(result.getSize()).isEqualTo(10);
    assertThat(result.getTotalElements()).isEqualTo(1L);
    assertThat(result.getTotalPages()).isEqualTo(1);
    assertThat(result.isLast()).isTrue();
  }

  @Test
  void pagedDocumentResponse_equalsAndHashCode_symmetric() {
    PagedDocumentResponse a =
        PagedDocumentResponse.builder()
            .content(List.of(response))
            .page(0)
            .size(10)
            .totalElements(1)
            .totalPages(1)
            .last(true)
            .build();
    PagedDocumentResponse b =
        PagedDocumentResponse.builder()
            .content(List.of(response))
            .page(0)
            .size(10)
            .totalElements(1)
            .totalPages(1)
            .last(true)
            .build();

    assertThat(a).isEqualTo(b);
    assertThat(a.hashCode()).isEqualTo(b.hashCode());
    assertThat(a).isNotEqualTo(null);
    assertThat(a).isNotEqualTo("other");
  }

  @Test
  void pagedDocumentResponse_notEqualWhenFieldsDiffer() {
    PagedDocumentResponse a =
        PagedDocumentResponse.builder()
            .content(List.of())
            .page(0)
            .size(10)
            .totalElements(0)
            .totalPages(0)
            .last(true)
            .build();
    PagedDocumentResponse b =
        PagedDocumentResponse.builder()
            .content(List.of())
            .page(1)
            .size(5)
            .totalElements(20)
            .totalPages(4)
            .last(false)
            .build();

    assertThat(a).isNotEqualTo(b);
  }

  @Test
  void pagedDocumentResponse_toString_containsClassName() {
    PagedDocumentResponse paged =
        PagedDocumentResponse.builder()
            .content(List.of())
            .page(0)
            .size(10)
            .totalElements(0)
            .totalPages(0)
            .last(true)
            .build();

    assertThat(paged.toString()).contains("PagedDocumentResponse");
  }

  // ── DownloadUrlResponse ───────────────────────────────────────────────────

  @Test
  void downloadUrlResponse_builder_setsAllFields() {
    UUID docId = UUID.randomUUID();
    DownloadUrlResponse url =
        DownloadUrlResponse.builder()
            .documentId(docId)
            .url("https://minio/signed?token=abc")
            .expiresInMinutes(60)
            .build();

    assertThat(url.getDocumentId()).isEqualTo(docId);
    assertThat(url.getUrl()).isEqualTo("https://minio/signed?token=abc");
    assertThat(url.getExpiresInMinutes()).isEqualTo(60);
  }

  @Test
  void downloadUrlResponse_equalsAndHashCode_symmetric() {
    UUID docId = UUID.randomUUID();
    DownloadUrlResponse a =
        DownloadUrlResponse.builder()
            .documentId(docId)
            .url("http://x")
            .expiresInMinutes(30)
            .build();
    DownloadUrlResponse b =
        DownloadUrlResponse.builder()
            .documentId(docId)
            .url("http://x")
            .expiresInMinutes(30)
            .build();

    assertThat(a).isEqualTo(b);
    assertThat(a.hashCode()).isEqualTo(b.hashCode());
    assertThat(a).isNotEqualTo(null);
    assertThat(a).isNotEqualTo("other");
  }

  @Test
  void downloadUrlResponse_notEqualWhenUrlDiffers() {
    UUID docId = UUID.randomUUID();
    DownloadUrlResponse a =
        DownloadUrlResponse.builder()
            .documentId(docId)
            .url("http://a")
            .expiresInMinutes(60)
            .build();
    DownloadUrlResponse b =
        DownloadUrlResponse.builder()
            .documentId(docId)
            .url("http://b")
            .expiresInMinutes(60)
            .build();

    assertThat(a).isNotEqualTo(b);
  }

  @Test
  void downloadUrlResponse_toString_containsUrl() {
    DownloadUrlResponse url =
        DownloadUrlResponse.builder()
            .documentId(UUID.randomUUID())
            .url("https://minio/doc")
            .expiresInMinutes(60)
            .build();

    assertThat(url.toString()).contains("https://minio/doc");
  }
}
