package com.clara.documentmanagement.repository;

import com.clara.documentmanagement.model.Document;
import jakarta.persistence.criteria.JoinType;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class DocumentSpecification {

  private static final String FIELD_USER_ID = "userId";
  private static final String FIELD_DOCUMENT_NAME = "documentName";
  private static final String FIELD_TAGS = "tags";

  private DocumentSpecification() {}

  public static Specification<Document> build(
      String userId, String documentName, List<String> tags) {
    return Specification.where(withUserId(userId))
        .and(withDocumentName(documentName))
        .and(withAnyTag(tags));
  }

  private static Specification<Document> withUserId(String userId) {
    return (root, query, cb) -> {
      if (userId == null || userId.isBlank()) return null;
      return cb.equal(root.get(FIELD_USER_ID), userId);
    };
  }

  private static Specification<Document> withDocumentName(String documentName) {
    return (root, query, cb) -> {
      if (documentName == null || documentName.isBlank()) return null;
      return cb.like(
          cb.lower(root.get(FIELD_DOCUMENT_NAME)), "%" + documentName.toLowerCase() + "%");
    };
  }

  /**
   * Matches documents that contain at least one of the requested tags. Uses JOIN so that documents
   * without any matching tag are excluded. query.distinct(true) prevents duplicate rows from the
   * join.
   */
  private static Specification<Document> withAnyTag(List<String> tags) {
    return (root, query, cb) -> {
      if (tags == null || tags.isEmpty()) return null;
      query.distinct(true);
      return root.join(FIELD_TAGS, JoinType.INNER).in(tags);
    };
  }
}
