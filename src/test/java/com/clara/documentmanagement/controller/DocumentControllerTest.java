package com.clara.documentmanagement.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.clara.documentmanagement.dto.response.DocumentResponse;
import com.clara.documentmanagement.dto.response.DownloadUrlResponse;
import com.clara.documentmanagement.dto.response.PagedDocumentResponse;
import com.clara.documentmanagement.exception.DocumentNotFoundException;
import com.clara.documentmanagement.exception.GlobalExceptionHandler;
import com.clara.documentmanagement.service.DocumentService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class DocumentControllerTest {

  private static final String BASE_URL = "/api/v1/documents";

  @Mock private DocumentService documentService;

  @InjectMocks private DocumentController documentController;

  private MockMvc mockMvc;
  private UUID documentId;
  private DocumentResponse sampleResponse;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(documentController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    documentId = UUID.randomUUID();
    sampleResponse =
        DocumentResponse.builder()
            .id(documentId)
            .userId("user1")
            .documentName("test.pdf")
            .tags(List.of("finance"))
            .fileSize(1024L)
            .fileType("application/pdf")
            .createdAt(LocalDateTime.now())
            .build();
  }

  // ── POST /api/v1/documents ─────────────────────────────────────────────────

  @Test
  void upload_validRequest_returns201() throws Exception {
    when(documentService.upload(eq("user1"), eq("test.pdf"), any(), any()))
        .thenReturn(sampleResponse);

    MockMultipartFile pdf =
        new MockMultipartFile("file", "test.pdf", "application/pdf", new byte[100]);

    mockMvc
        .perform(
            multipart(BASE_URL)
                .file(pdf)
                .param("userId", "user1")
                .param("documentName", "test.pdf")
                .param("tags", "finance"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.userId").value("user1"))
        .andExpect(jsonPath("$.documentName").value("test.pdf"));
  }

  @Test
  void upload_missingUserId_returns400() throws Exception {
    MockMultipartFile pdf =
        new MockMultipartFile("file", "test.pdf", "application/pdf", new byte[100]);

    mockMvc
        .perform(multipart(BASE_URL).file(pdf).param("documentName", "test.pdf")) // missing userId
        .andExpect(status().isBadRequest());
  }

  @Test
  void upload_missingFile_returns400() throws Exception {
    mockMvc
        .perform(multipart(BASE_URL).param("userId", "user1").param("documentName", "test.pdf"))
        .andExpect(status().isBadRequest());
  }

  // ── GET /api/v1/documents ──────────────────────────────────────────────────

  @Test
  void search_noFilters_returns200() throws Exception {
    PagedDocumentResponse paged =
        PagedDocumentResponse.builder()
            .content(List.of(sampleResponse))
            .page(0)
            .size(10)
            .totalElements(1)
            .totalPages(1)
            .last(true)
            .build();

    when(documentService.search(isNull(), isNull(), isNull(), eq(0), eq(10))).thenReturn(paged);

    mockMvc
        .perform(get(BASE_URL)) // no userId → null, returns all
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  void search_withFilters_passesParamsToService() throws Exception {
    PagedDocumentResponse empty =
        PagedDocumentResponse.builder()
            .content(List.of())
            .page(0)
            .size(5)
            .totalElements(0)
            .totalPages(0)
            .last(true)
            .build();

    when(documentService.search(eq("user1"), eq("test"), any(), eq(0), eq(5))).thenReturn(empty);

    mockMvc
        .perform(
            get(BASE_URL).param("userId", "user1").param("documentName", "test").param("size", "5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(0));
  }

  // ── GET /api/v1/documents/{id}/download ────────────────────────────────────

  @Test
  void download_existingId_returnsPresignedUrl() throws Exception {
    DownloadUrlResponse urlResponse =
        DownloadUrlResponse.builder()
            .documentId(documentId)
            .url("https://minio/signed")
            .expiresInMinutes(60)
            .build();

    when(documentService.generateDownloadUrl(documentId)).thenReturn(urlResponse);

    mockMvc
        .perform(get(BASE_URL + "/{id}/download", documentId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.url").value("https://minio/signed"))
        .andExpect(jsonPath("$.expiresInMinutes").value(60));
  }

  @Test
  void download_missingId_returns404() throws Exception {
    UUID missing = UUID.randomUUID();
    when(documentService.generateDownloadUrl(missing))
        .thenThrow(new DocumentNotFoundException(missing));

    mockMvc
        .perform(get(BASE_URL + "/{id}/download", missing))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  void download_invalidUuid_returns400() throws Exception {
    mockMvc
        .perform(get(BASE_URL + "/{id}/download", "not-a-uuid"))
        .andExpect(status().isBadRequest());
  }
}
