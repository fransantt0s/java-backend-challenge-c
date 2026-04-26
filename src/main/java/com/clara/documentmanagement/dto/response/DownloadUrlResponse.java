package com.clara.documentmanagement.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Schema(description = "Temporary download URL for a document")
public class DownloadUrlResponse {

  @Schema(description = "ID of the document", example = "550e8400-e29b-41d4-a716-446655440000")
  UUID documentId;

  @Schema(description = "Pre-signed MinIO URL valid for the configured expiry window")
  String url;

  @Schema(description = "Number of minutes until the URL expires", example = "60")
  int expiresInMinutes;
}
