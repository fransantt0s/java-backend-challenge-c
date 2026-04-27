package com.clara.documentmanagement.exception;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.UUID;

class GlobalExceptionHandlerTest {

  private GlobalExceptionHandler handler;

  @BeforeEach
  void setUp() {
    handler = new GlobalExceptionHandler();
  }

  // ── DocumentNotFoundException ─────────────────────────────────────────────

  @Test
  void handleNotFound_returns404WithMessage() {
    UUID id = UUID.randomUUID();
    DocumentNotFoundException ex = new DocumentNotFoundException(id);

    ResponseEntity<ErrorResponse> response = handler.handleNotFound(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().message()).contains(id.toString());
  }

  // ── StorageException ──────────────────────────────────────────────────────

  @Test
  void handleStorage_returns500() {
    StorageException ex = new StorageException("bucket write failed", new RuntimeException());

    ResponseEntity<ErrorResponse> response = handler.handleStorage(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody().message()).contains("Storage operation failed");
  }

  // ── MaxUploadSizeExceededException ────────────────────────────────────────

  @Test
  void handleMaxUpload_returns413WithFixedMessage() {
    MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(500 * 1024 * 1024L);

    ResponseEntity<ErrorResponse> response = handler.handleMaxUpload(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
    assertThat(response.getBody().message()).contains("500 MB");
  }

  // ── IllegalArgumentException ──────────────────────────────────────────────

  @Test
  void handleIllegalArgument_returns400WithMessage() {
    IllegalArgumentException ex = new IllegalArgumentException("only PDF files are accepted");

    ResponseEntity<ErrorResponse> response = handler.handleIllegalArgument(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().message()).contains("only PDF files are accepted");
  }

  // ── MissingServletRequestParameterException ───────────────────────────────

  @Test
  void handleMissingParam_returns400WithParamName() throws Exception {
    MissingServletRequestParameterException ex =
        new MissingServletRequestParameterException("userId", "String");

    ResponseEntity<ErrorResponse> response = handler.handleMissingParam(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().message()).contains("userId");
  }

  // ── MissingServletRequestPartException ───────────────────────────────────

  @Test
  void handleMissingPart_returns400WithPartName() {
    MissingServletRequestPartException ex = new MissingServletRequestPartException("file");

    ResponseEntity<ErrorResponse> response = handler.handleMissingPart(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().message()).contains("file");
  }

  // ── MethodArgumentTypeMismatchException ──────────────────────────────────

  @Test
  void handleTypeMismatch_returns400WithDetails() {
    MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
    when(ex.getName()).thenReturn("id");
    when(ex.getValue()).thenReturn("not-a-uuid");

    ResponseEntity<ErrorResponse> response = handler.handleTypeMismatch(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().message()).contains("id").contains("not-a-uuid");
  }

  // ── ConstraintViolationException ──────────────────────────────────────────

  @Test
  @SuppressWarnings("unchecked")
  void handleConstraintViolation_returns400WithViolationDetails() {
    ConstraintViolation<?> violation = mock(ConstraintViolation.class);
    jakarta.validation.Path path = mock(jakarta.validation.Path.class);
    when(path.toString()).thenReturn("upload.userId");
    when(violation.getPropertyPath()).thenReturn(path);
    when(violation.getMessage()).thenReturn("must not be blank");

    ConstraintViolationException ex = new ConstraintViolationException(Set.of(violation));

    ResponseEntity<ErrorResponse> response = handler.handleConstraintViolation(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().message()).contains("must not be blank");
  }

  // ── MethodArgumentNotValidException ──────────────────────────────────────

  @Test
  void handleValidation_returns400WithFieldErrors() {
    MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
    BindingResult bindingResult = mock(BindingResult.class);
    FieldError fieldError = new FieldError("request", "documentName", "must not be blank");

    when(ex.getBindingResult()).thenReturn(bindingResult);
    when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

    ResponseEntity<ErrorResponse> response = handler.handleValidation(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().message()).contains("documentName").contains("must not be blank");
  }

  // ── NoResourceFoundException ──────────────────────────────────────────────

  @Test
  void handleNoResource_returns404WithPath() {
    NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.GET, "/unknown/route");

    ResponseEntity<ErrorResponse> response = handler.handleNoResource(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody().message()).contains("/unknown/route");
  }

  // ── Generic Exception ─────────────────────────────────────────────────────

  @Test
  void handleGeneric_returns500WithGenericMessage() {
    Exception ex = new RuntimeException("something went very wrong");

    ResponseEntity<ErrorResponse> response = handler.handleGeneric(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody().message()).isEqualTo("An unexpected error occurred");
  }
}
