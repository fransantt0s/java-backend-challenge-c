package com.clara.documentmanagement.dto.response;

import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DownloadUrlResponse {

  UUID documentId;
  String url;
  int expiresInMinutes;
}
