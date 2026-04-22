package com.renansouza.transactions;

import com.renansouza.transactions.domain.TransactionEntity;
import com.renansouza.transactions.repository.TransactionSpecification;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.homewealth.transactions.model.OperationType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class TransactionSpecificationTest {

  @Mock
  private Root<TransactionEntity> root;
  @Mock
  private CriteriaQuery<?> query;
  @Mock
  private CriteriaBuilder criteriaBuilder;
  @Mock
  private Path<Object> path;
  @Mock
  private Predicate predicate;

  @BeforeEach
  void setUp() {
    lenient().when(root.get(anyString())).thenReturn(path);
    lenient().when(criteriaBuilder.equal(any(), any())).thenReturn(predicate);
    lenient().when(criteriaBuilder.greaterThanOrEqualTo(any(), any(LocalDate.class)))
        .thenReturn(predicate);
    lenient().when(criteriaBuilder.lessThanOrEqualTo(any(), any(LocalDate.class)))
        .thenReturn(predicate);
    lenient().when(criteriaBuilder.and(any(Predicate[].class))).thenReturn(predicate);
  }

  @Test
  void constructor_shouldThrowUnsupportedOperationException() throws Exception {
    var constructor = TransactionSpecification.class.getDeclaredConstructor();
    constructor.setAccessible(true);
    assertThatThrownBy(constructor::newInstance).cause()
        .isInstanceOf(UnsupportedOperationException.class).hasMessage("Utility class");
  }

  @Test
  void withFilters_allNull_shouldProduceNoPredicates() {
    Specification<TransactionEntity> spec = TransactionSpecification
        .withFilters(null, null, null, null, null);

    spec.toPredicate(root, query, criteriaBuilder);

    verify(criteriaBuilder).and();
    verify(criteriaBuilder, never()).equal(any(), any());
    verify(criteriaBuilder, never()).greaterThanOrEqualTo(any(), any(LocalDate.class));
    verify(criteriaBuilder, never()).lessThanOrEqualTo(any(), any(LocalDate.class));
  }

  @Test
  void withFilters_blankStrings_shouldProduceNoPredicates() {
    Specification<TransactionEntity> spec = TransactionSpecification
        .withFilters("   ", "  ", null, null, null);

    spec.toPredicate(root, query, criteriaBuilder);

    verify(criteriaBuilder).and();
    verify(criteriaBuilder, never()).equal(any(), any());
  }

  @Test
  void withFilters_validPortfolioId_shouldAddEqualPredicate() {
    String portfolioId = UUID.randomUUID().toString();

    Specification<TransactionEntity> spec = TransactionSpecification
        .withFilters(portfolioId, null, null, null, null);

    spec.toPredicate(root, query, criteriaBuilder);

    verify(root).get("portfolioId");
    verify(criteriaBuilder).equal(path, UUID.fromString(portfolioId));
  }

  @Test
  void withFilters_invalidPortfolioIdFormat_shouldThrowIllegalArgumentException() {
    Specification<TransactionEntity> spec = TransactionSpecification
        .withFilters("not-a-uuid", null, null, null, null);

    assertThatThrownBy(() -> spec.toPredicate(root, query, criteriaBuilder)).isInstanceOf(
        IllegalArgumentException.class);
  }

  @Test
  void withFilters_validAssetId_shouldAddEqualPredicate() {
    Specification<TransactionEntity> spec = TransactionSpecification
        .withFilters(null, "ASSET4", null, null, null);

    spec.toPredicate(root, query, criteriaBuilder);

    verify(root).get("assetId");
    verify(criteriaBuilder).equal(path, "ASSET4");
  }

  @Test
  void withFilters_emptyAssetId_shouldSkipPredicate() {
    Specification<TransactionEntity> spec = TransactionSpecification
        .withFilters(null, "", null, null, null);

    spec.toPredicate(root, query, criteriaBuilder);

    verify(root, never()).get("assetId");
  }

  @Test
  void withFilters_fromDate_shouldAddGreaterThanOrEqualPredicate() {
    LocalDate fromDate = LocalDate.of(2024, 1, 1);

    Specification<TransactionEntity> spec = TransactionSpecification
        .withFilters(null, null, fromDate, null, null);

    spec.toPredicate(root, query, criteriaBuilder);

    verify(root).get("operationDate");
    verify(criteriaBuilder).greaterThanOrEqualTo(ArgumentMatchers.<Expression<LocalDate>>any(),
        eq(fromDate));
  }

  @Test
  void withFilters_toDate_shouldAddLessThanOrEqualPredicate() {
    LocalDate toDate = LocalDate.of(2024, 12, 31);

    Specification<TransactionEntity> spec = TransactionSpecification
        .withFilters(null, null, null, toDate, null);

    spec.toPredicate(root, query, criteriaBuilder);

    verify(root).get("operationDate");
    verify(criteriaBuilder).lessThanOrEqualTo(ArgumentMatchers.<Expression<LocalDate>>any(),
        eq(toDate));
  }

  @Test
  void withFilters_dateRange_shouldAddBothDatePredicates() {
    LocalDate fromDate = LocalDate.of(2024, 1, 1);
    LocalDate toDate = LocalDate.of(2024, 12, 31);

    Specification<TransactionEntity> spec = TransactionSpecification
        .withFilters(null, null, fromDate, toDate, null);

    spec.toPredicate(root, query, criteriaBuilder);

    verify(criteriaBuilder).greaterThanOrEqualTo(ArgumentMatchers.<Expression<LocalDate>>any(),
        eq(fromDate));
    verify(criteriaBuilder).lessThanOrEqualTo(ArgumentMatchers.<Expression<LocalDate>>any(),
        eq(toDate));
  }

  @Test
  void withFilters_operationTypeBuy_shouldAddEqualPredicate() {
    Specification<TransactionEntity> spec = TransactionSpecification
        .withFilters(null, null, null, null, OperationType.BUY);

    spec.toPredicate(root, query, criteriaBuilder);

    verify(root).get("operationType");
    verify(criteriaBuilder).equal(path, OperationType.BUY);
  }

  @Test
  void withFilters_operationTypeSell_shouldAddEqualPredicate() {
    Specification<TransactionEntity> spec = TransactionSpecification
        .withFilters(null, null, null, null, OperationType.SELL);

    spec.toPredicate(root, query, criteriaBuilder);

    verify(root).get("operationType");
    verify(criteriaBuilder).equal(path, OperationType.SELL);
  }

  @Test
  void withFilters_allFiltersProvided_shouldAddAllPredicates() {
    String portfolioId = UUID.randomUUID().toString();
    String assetId = "ASSET-XYZ";
    LocalDate fromDate = LocalDate.of(2024, 1, 1);
    LocalDate toDate = LocalDate.of(2024, 6, 30);

    Specification<TransactionEntity> spec = TransactionSpecification
        .withFilters(portfolioId, assetId, fromDate, toDate, OperationType.BUY);

    spec.toPredicate(root, query, criteriaBuilder);

    verify(criteriaBuilder).equal(path, UUID.fromString(portfolioId));
    verify(criteriaBuilder).equal(path, assetId);
    verify(criteriaBuilder).greaterThanOrEqualTo(ArgumentMatchers.<Expression<LocalDate>>any(), eq(fromDate));
    verify(criteriaBuilder).lessThanOrEqualTo(ArgumentMatchers.<Expression<LocalDate>>any(), eq(toDate));
    verify(criteriaBuilder).equal(path, OperationType.BUY);

    verify(criteriaBuilder).and(any(Predicate[].class));
  }
}