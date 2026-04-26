package com.clara.documentmanagement.config;

import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MinioProperties.class)
@RequiredArgsConstructor
public class MinioConfig {

  private final MinioProperties minioProperties;

  @Bean
  public MinioClient minioClient() {
    return MinioClient.builder()
        .endpoint(minioProperties.getEndpoint())
        .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
        .build();
  }

  /**
   * Separate client for presigned URLs so the signature is computed with the public host.
   * Region is set explicitly to avoid the SDK making a network call to the endpoint to detect it —
   * which would fail when publicEndpoint (e.g. localhost:9000) is unreachable from inside Docker.
   */
  @Bean
  @Qualifier("presigned")
  public MinioClient presignedMinioClient() {
    String endpoint =
        (minioProperties.getPublicEndpoint() != null
                && !minioProperties.getPublicEndpoint().isBlank())
            ? minioProperties.getPublicEndpoint()
            : minioProperties.getEndpoint();
    return MinioClient.builder()
        .endpoint(endpoint)
        .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
        .region("us-east-1")
        .build();
  }
}
