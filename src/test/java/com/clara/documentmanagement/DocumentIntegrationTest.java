package com.clara.documentmanagement;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.clara.documentmanagement.repository.DocumentRepository;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class DocumentIntegrationTest {

  private static final String BASE_URL = "/api/v1/documents";
  private static final byte[] PDF_CONTENT = "%PDF-1.4\n%%EOF\n".getBytes(StandardCharsets.UTF_8);

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

  @Container
  @SuppressWarnings("resource")
  static GenericContainer<?> minio =
      new GenericContainer<>("minio/minio:latest")
          .withEnv("MINIO_ROOT_USER", "minioadmin")
          .withEnv("MINIO_ROOT_PASSWORD", "minioadmin")
          .withCommand("server /data")
          .withExposedPorts(9000)
          .waitingFor(Wait.forHttp("/minio/health/live").forPort(9000));

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add(
        "minio.endpoint", () -> "http://" + minio.getHost() + ":" + minio.getMappedPort(9000));
    registry.add("minio.access-key", () -> "minioadmin");
    registry.add("minio.secret-key", () -> "minioadmin");
    registry.add("minio.bucket", () -> "test-bucket");
  }

  @Autowired MockMvc mockMvc;
  @Autowired DocumentRepository documentRepository;

  @AfterEach
  void cleanup() {
    documentRepository.deleteAll();
  }

  @Test
  void upload_thenSearch_thenDownload_fullFlow() throws Exception {
    MockMultipartFile pdf =
        new MockMultipartFile("file", "report.pdf", "application/pdf", PDF_CONTENT);

    String uploadBody =
        mockMvc
            .perform(
                multipart(BASE_URL)
                    .file(pdf)
                    .param("userId", "alice")
                    .param("documentName", "report.pdf")
                    .param("tags", "finance")
                    .param("tags", "2024"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", notNullValue()))
            .andExpect(jsonPath("$.userId").value("alice"))
            .andExpect(jsonPath("$.documentName").value("report.pdf"))
            .andExpect(jsonPath("$.tags").isArray())
            .andReturn()
            .getResponse()
            .getContentAsString();

    String documentId = JsonPath.read(uploadBody, "$.id");

    mockMvc
        .perform(get(BASE_URL).param("userId", "alice"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].documentName").value("report.pdf"));

    mockMvc
        .perform(get(BASE_URL + "/{id}/download", documentId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.url", notNullValue()))
        .andExpect(jsonPath("$.documentId").value(documentId))
        .andExpect(jsonPath("$.expiresInMinutes").value(10));
  }

  @Test
  void upload_nonPdfContentType_returns400() throws Exception {
    mockMvc
        .perform(
            multipart(BASE_URL)
                .file(new MockMultipartFile("file", "img.png", "image/png", new byte[64]))
                .param("userId", "bob")
                .param("documentName", "img.png"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", containsString("PDF")));
  }

  @Test
  void upload_pdfContentTypeButInvalidMagicNumber_returns400() throws Exception {
    mockMvc
        .perform(
            multipart(BASE_URL)
                .file(new MockMultipartFile("file", "fake.pdf", "application/pdf", new byte[64]))
                .param("userId", "bob")
                .param("documentName", "fake.pdf"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", containsString("%PDF")));
  }

  @Test
  void search_noMatchingDocuments_returnsEmptyPage() throws Exception {
    mockMvc
        .perform(get(BASE_URL).param("userId", "nobody"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(0))
        .andExpect(jsonPath("$.content").isEmpty());
  }

  @Test
  void download_nonExistentId_returns404() throws Exception {
    mockMvc
        .perform(get(BASE_URL + "/{id}/download", UUID.randomUUID()))
        .andExpect(status().isNotFound());
  }
}
