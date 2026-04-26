package com.clara.documentmanagement.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "minio")
@Validated
@Getter
@Setter
public class MinioProperties {

  @NotBlank private String endpoint;
  private String publicEndpoint; // external URL for presigned URLs; defaults to endpoint
  @NotBlank private String accessKey;
  @NotBlank private String secretKey;
  @NotBlank private String bucket;

  @Positive private int presignedUrlExpiryMinutes = 60;
}
