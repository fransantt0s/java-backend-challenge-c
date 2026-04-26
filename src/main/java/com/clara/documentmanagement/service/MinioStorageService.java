package com.clara.documentmanagement.service;

import com.clara.documentmanagement.config.MinioProperties;
import com.clara.documentmanagement.exception.StorageException;
import io.minio.*;
import io.minio.http.Method;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MinioStorageService implements StorageService {

  private final MinioClient minioClient;
  private final MinioClient presignedMinioClient;
  private final MinioProperties minioProperties;

  public MinioStorageService(
      MinioClient minioClient,
      @Qualifier("presigned") MinioClient presignedMinioClient,
      MinioProperties minioProperties) {
    this.minioClient = minioClient;
    this.presignedMinioClient = presignedMinioClient;
    this.minioProperties = minioProperties;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void ensureBucketExists() {
    try {
      boolean exists =
          minioClient.bucketExists(
              BucketExistsArgs.builder().bucket(minioProperties.getBucket()).build());
      if (!exists) {
        minioClient.makeBucket(
            MakeBucketArgs.builder().bucket(minioProperties.getBucket()).build());
        log.info("Created MinIO bucket: {}", minioProperties.getBucket());
      }
    } catch (Exception ex) {
      throw new IllegalStateException(
          "Failed to ensure MinIO bucket '" + minioProperties.getBucket() + "' exists", ex);
    }
  }

  /**
   * Streams the file to MinIO using the MinIO SDK's internal multipart upload for large objects.
   * The SDK reads the stream in configurable chunks — heap usage remains bounded regardless of file
   * size.
   */
  @Override
  public void store(
      String objectKey, InputStream inputStream, long contentLength, String contentType) {
    try {
      minioClient.putObject(
          PutObjectArgs.builder().bucket(minioProperties.getBucket()).object(objectKey).stream(
                  inputStream, contentLength, -1) // -1 → SDK auto-selects part size
              .contentType(contentType)
              .build());
      log.info("Stored object: {}/{}", minioProperties.getBucket(), objectKey);
    } catch (Exception ex) {
      throw new StorageException("Failed to store object: " + objectKey, ex);
    }
  }

  @Override
  public String generatePresignedUrl(String objectKey) {
    try {
      return presignedMinioClient.getPresignedObjectUrl(
          GetPresignedObjectUrlArgs.builder()
              .method(Method.GET)
              .bucket(minioProperties.getBucket())
              .object(objectKey)
              .expiry(minioProperties.getPresignedUrlExpiryMinutes(), TimeUnit.MINUTES)
              .build());
    } catch (Exception ex) {
      throw new StorageException("Failed to generate presigned URL for: " + objectKey, ex);
    }
  }
}
