package com.clara.documentmanagement.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.*;

@Entity
@Table(
    name = "documents",
    indexes = {
      @Index(name = "idx_documents_user_id", columnList = "user_id"),
      @Index(name = "idx_documents_document_name", columnList = "document_name"),
      @Index(name = "idx_documents_created_at", columnList = "created_at")
    })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private String userId;

  @Column(name = "document_name", nullable = false)
  private String documentName;

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(
      name = "document_tags",
      joinColumns = @JoinColumn(name = "document_id"),
      indexes = @Index(name = "idx_document_tags_tag", columnList = "tag"))
  @Column(name = "tag", nullable = false)
  @Builder.Default
  private List<String> tags = new ArrayList<>();

  @Column(name = "minio_path", nullable = false, length = 500)
  private String minioPath;

  @Column(name = "file_size", nullable = false)
  private Long fileSize;

  @Column(name = "file_type", nullable = false)
  private String fileType;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    if (createdAt == null) {
      createdAt = LocalDateTime.now();
    }
  }
}
