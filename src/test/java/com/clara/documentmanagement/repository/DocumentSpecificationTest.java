package com.clara.documentmanagement.repository;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.clara.documentmanagement.model.Document;
import jakarta.persistence.criteria.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
class DocumentSpecificationTest {

  @Mock private Root<Document> root;
  @Mock private CriteriaQuery<?> query;
  @Mock private CriteriaBuilder cb;
  @Mock private Predicate predicate;
  @Mock private Expression<String> lowerExpression;
  @Mock private Join<Document, String> join;

  // ── null / blank guards ───────────────────────────────────────────────────

  @Test
  void build_allFiltersNull_returnsNullPredicate() {
    Predicate result = DocumentSpecification.build(null, null, null).toPredicate(root, query, cb);
    assertThat(result).isNull();
  }

  @Test
  void build_blankUserId_treatedAsAbsent() {
    Predicate result = DocumentSpecification.build("  ", null, null).toPredicate(root, query, cb);
    assertThat(result).isNull();
  }

  @Test
  void build_blankDocumentName_treatedAsAbsent() {
    Predicate result = DocumentSpecification.build(null, "", null).toPredicate(root, query, cb);
    assertThat(result).isNull();
  }

  @Test
  void build_emptyTagList_treatedAsAbsent() {
    Predicate result =
        DocumentSpecification.build(null, null, List.of()).toPredicate(root, query, cb);
    assertThat(result).isNull();
  }

  // ── userId filter ─────────────────────────────────────────────────────────

  @Test
  void build_withUserId_delegatesToCbEqual() {
    when(root.get("userId")).thenReturn(mock(Path.class));
    when(cb.equal(any(), eq("user-42"))).thenReturn(predicate);

    Predicate result =
        DocumentSpecification.build("user-42", null, null).toPredicate(root, query, cb);

    assertThat(result).isEqualTo(predicate);
    verify(cb).equal(any(), eq("user-42"));
  }

  // ── documentName filter ───────────────────────────────────────────────────

  @Test
  void build_withDocumentName_usesLowercaseLike() {
    when(root.get("documentName")).thenReturn(mock(Path.class));
    when(cb.lower(any())).thenReturn(lowerExpression);
    when(cb.like(eq(lowerExpression), eq("%invoice%"))).thenReturn(predicate);

    Predicate result =
        DocumentSpecification.build(null, "Invoice", null).toPredicate(root, query, cb);

    assertThat(result).isEqualTo(predicate);
    verify(cb).like(lowerExpression, "%invoice%");
  }

  // ── tags filter ───────────────────────────────────────────────────────────

  @Test
  void build_withTags_joinsAndSetsDistinct() {
    when(root.join("tags", JoinType.INNER)).thenReturn((Join) join);
    when(join.in(List.of("finance", "legal"))).thenReturn(predicate);

    Predicate result =
        DocumentSpecification.build(null, null, List.of("finance", "legal"))
            .toPredicate(root, query, cb);

    assertThat(result).isEqualTo(predicate);
    verify(query).distinct(true);
    verify(root).join("tags", JoinType.INNER);
  }
}
