package com.clara.documentmanagement.controller;

import com.clara.documentmanagement.dto.response.DocumentResponse;
import com.clara.documentmanagement.dto.response.DownloadUrlResponse;
import com.clara.documentmanagement.dto.response.PagedDocumentResponse;
import com.clara.documentmanagement.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Validated
@Tag(name = "Documents", description = "Document management operations")
public class DocumentController {

  private final DocumentService documentService;

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "Upload a PDF document",
      description =
          "Accepts a PDF up to 500 MB. The file is streamed directly to MinIO — heap usage stays"
              + " bounded regardless of file size.",
      responses = {
        @ApiResponse(responseCode = "201", description = "Document uploaded successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request or non-PDF file"),
        @ApiResponse(responseCode = "413", description = "File exceeds 500 MB limit")
      })
  public DocumentResponse upload(
      @RequestParam @NotBlank String userId,
      @RequestParam @NotBlank String documentName,
      @RequestParam(required = false) List<String> tags,
      @RequestParam("file")
          @NotNull
          @Parameter(
              description = "PDF file to upload",
              content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE))
          MultipartFile file) {

    return documentService.upload(userId, documentName, tags, file);
  }

  @GetMapping
  @Operation(
      summary = "Search documents",
      description =
          "Filters documents by user, name, and/or tags. Returns paginated results sorted by"
              + " upload date descending. No download URL is included.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Search results",
            content = @Content(schema = @Schema(implementation = PagedDocumentResponse.class)))
      })
  public PagedDocumentResponse search(
      @RequestParam(required = false) String userId,
      @RequestParam(required = false) String documentName,
      @RequestParam(required = false) List<String> tags,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "10") @Min(1) int size) {

    return documentService.search(userId, documentName, tags, page, size);
  }

  @GetMapping("/{id}/download")
  @Operation(
      summary = "Get a temporary download URL",
      description =
          "Generates a time-limited pre-signed MinIO URL that allows the caller to download the"
              + " document without exposing storage credentials.",
      responses = {
        @ApiResponse(responseCode = "200", description = "Presigned URL generated"),
        @ApiResponse(responseCode = "404", description = "Document not found")
      })
  public DownloadUrlResponse download(@PathVariable UUID id) {
    return documentService.generateDownloadUrl(id);
  }
}
