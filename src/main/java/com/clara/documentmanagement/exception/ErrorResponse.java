package com.clara.documentmanagement.exception;

import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;

public record ErrorResponse(String timestamp, int status, String error, String message) {

  public static ErrorResponse of(HttpStatus status, String message) {
    return new ErrorResponse(
        LocalDateTime.now().toString(), status.value(), status.getReasonPhrase(), message);
  }
}
