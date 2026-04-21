package com.renansouza.transactions.repository;

import com.renansouza.transactions.domain.TransactionEntity;

import com.homewealth.transactions.model.OperationType;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/**
 * Utility class providing JPA {@link Specification} factories for filtering
 * {@link TransactionEntity} queries dynamically.
 *
 * <p>All filter parameters are optional. When {@code null} (or blank, for strings),
 * the corresponding predicate is omitted, effectively acting as a wildcard.
 *
 * <p>This class is not instantiable; use the static factory method directly:
 * <pre>{@code
 * Specification<TransactionEntity> spec = TransactionSpecification.withFilters(
 *     portfolioId, assetId, fromDate, toDate, operationType);
 * repository.findAll(spec, pageable);
 * }</pre>
 *
 * @author Renan Alberto de Souza
 * @since 1.0
 */
public class TransactionSpecification {

  private TransactionSpecification() {
    throw new UnsupportedOperationException("Utility class");
  }

  /**
   * Builds a {@link Specification} that combines all provided filters using AND logic.
   *
   * <p>Each parameter is independently optional:
   * <ul>
   *   <li>{@code portfolioId} — filters by portfolio UUID (parsed from string); ignored if null or blank</li>
   *   <li>{@code assetId} — filters by asset identifier; ignored if null or blank</li>
   *   <li>{@code fromDate} — lower bound on {@code operationDate} (inclusive); ignored if null</li>
   *   <li>{@code toDate} — upper bound on {@code operationDate} (inclusive); ignored if null</li>
   *   <li>{@code operationType} — filters by exact {@link OperationType}; ignored if null</li>
   * </ul>
   *
   * @param portfolioId   the portfolio UUID as a string, or {@code null}/blank to skip
   * @param assetId       the asset identifier, or {@code null}/blank to skip
   * @param fromDate      the start date (inclusive), or {@code null} to skip
   * @param toDate        the end date (inclusive), or {@code null} to skip
   * @param operationType the operation type to match, or {@code null} to skip
   * @return a {@link Specification} combining all active filters with AND; never {@code null}
   * @throws IllegalArgumentException if {@code portfolioId} is non-blank but not a valid UUID
   */
  public static Specification<TransactionEntity> withFilters(
      String portfolioId,
      String assetId,
      LocalDate fromDate,
      LocalDate toDate,
      OperationType operationType) {

    return (root, query, criteriaBuilder) -> {
      List<Predicate> predicates = new ArrayList<>();

      if (portfolioId != null && !portfolioId.isBlank()) {
        predicates.add(criteriaBuilder.equal(
            root.get("portfolioId"),
            UUID.fromString(portfolioId)
        ));
      }

      if (assetId != null && !assetId.isBlank()) {
        predicates.add(criteriaBuilder.equal(root.get("assetId"), assetId));
      }

      if (fromDate != null) {
        predicates.add(criteriaBuilder.greaterThanOrEqualTo(
            root.get("operationDate"),
            fromDate
        ));
      }

      if (toDate != null) {
        predicates.add(criteriaBuilder.lessThanOrEqualTo(
            root.get("operationDate"),
            toDate
        ));
      }

      if (operationType != null) {
        predicates.add(criteriaBuilder.equal(
            root.get("operationType"),
            operationType
        ));
      }

      return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    };
  }
}