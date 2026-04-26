package com.clara.documentmanagement.service;

import java.io.InputStream;

public interface StorageService {

  /**
   * Stores a file and returns the object key (path) within the bucket.
   *
   * @param objectKey unique key, e.g. "userId/docId/filename.pdf"
   * @param inputStream file content — consumed via streaming, never fully buffered
   * @param contentLength exact byte length of the stream
   * @param contentType MIME type
   */
  void store(String objectKey, InputStream inputStream, long contentLength, String contentType);

  /**
   * Generates a time-limited pre-signed GET URL for the given object key.
   *
   * @param objectKey same key used in {@link #store}
   * @return temporary HTTPS URL valid for the configured expiry period
   */
  String generatePresignedUrl(String objectKey);
}
