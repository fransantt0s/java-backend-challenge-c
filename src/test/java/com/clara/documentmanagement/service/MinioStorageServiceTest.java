package com.clara.documentmanagement.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.clara.documentmanagement.config.MinioProperties;
import com.clara.documentmanagement.exception.StorageException;
import io.minio.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MinioStorageServiceTest {

  @Mock private MinioClient minioClient;
  @Mock private MinioClient presignedMinioClient;
  @Mock private MinioProperties minioProperties;

  private MinioStorageService storageService;

  @BeforeEach
  void setUp() {
    storageService = new MinioStorageService(minioClient, presignedMinioClient, minioProperties);
    when(minioProperties.getBucket()).thenReturn("test-bucket");
  }

  // ── ensureBucketExists ────────────────────────────────────────────────────

  @Test
  void ensureBucketExists_createsBucketWhenItDoesNotExist() throws Exception {
    when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);

    storageService.ensureBucketExists();

    verify(minioClient).makeBucket(any(MakeBucketArgs.class));
  }

  @Test
  void ensureBucketExists_skipsCreationWhenBucketAlreadyExists() throws Exception {
    when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

    storageService.ensureBucketExists();

    verify(minioClient, never()).makeBucket(any());
  }

  @Test
  void ensureBucketExists_throwsIllegalStateExceptionOnConnectionFailure() throws Exception {
    when(minioClient.bucketExists(any(BucketExistsArgs.class)))
        .thenThrow(new RuntimeException("connection refused"));

    assertThatThrownBy(() -> storageService.ensureBucketExists())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("test-bucket");
  }

  // ── store ─────────────────────────────────────────────────────────────────

  @Test
  void store_callsPutObjectWithCorrectArgs() throws Exception {
    InputStream stream = new ByteArrayInputStream("pdf-content".getBytes());

    storageService.store("user/uuid/doc.pdf", stream, 11L, "application/pdf");

    verify(minioClient).putObject(any(PutObjectArgs.class));
  }

  @Test
  void store_wrapsExceptionAsStorageException() throws Exception {
    when(minioClient.putObject(any(PutObjectArgs.class)))
        .thenThrow(new RuntimeException("minio unavailable"));

    assertThatThrownBy(
            () ->
                storageService.store(
                    "key", new ByteArrayInputStream(new byte[0]), 0L, "application/pdf"))
        .isInstanceOf(StorageException.class)
        .hasMessageContaining("key");
  }

  // ── generatePresignedUrl ──────────────────────────────────────────────────

  @Test
  void generatePresignedUrl_returnsSignedUrl() throws Exception {
    when(minioProperties.getPresignedUrlExpiryMinutes()).thenReturn(60);
    when(presignedMinioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
        .thenReturn("http://localhost:9000/test-bucket/doc.pdf?X-Amz-Signature=abc");

    String url = storageService.generatePresignedUrl("user/uuid/doc.pdf");

    assertThat(url).startsWith("http://localhost:9000");
  }

  @Test
  void generatePresignedUrl_wrapsExceptionAsStorageException() throws Exception {
    when(minioProperties.getPresignedUrlExpiryMinutes()).thenReturn(60);
    when(presignedMinioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
        .thenThrow(new RuntimeException("signing failed"));

    assertThatThrownBy(() -> storageService.generatePresignedUrl("user/uuid/doc.pdf"))
        .isInstanceOf(StorageException.class)
        .hasMessageContaining("user/uuid/doc.pdf");
  }
}
