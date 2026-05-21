package com.renansouza.transactions.service;

import com.renansouza.transactions.domain.TransactionEntity;
import com.renansouza.transactions.domain.TransactionsMapper;
import com.renansouza.transactions.exception.TransactionNotFoundException;
import com.renansouza.transactions.repository.TransactionSpecification;
import com.renansouza.transactions.repository.TransactionsRepository;

import com.homewealth.transactions.model.OperationType;
import com.homewealth.transactions.model.TransactionPageResponse;
import com.homewealth.transactions.model.TransactionRequest;
import com.homewealth.transactions.model.TransactionResponse;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

/**
 * Service implementation for managing financial transactions operations.
 *
 * <p>This implementation is the core operations for creating, retrieving, and deleting
 * transactions within the system.
 *
 * @author Renan Alberto de Souza
 * @since 1.0
 */
@Service
public class TransactionsServiceImpl implements TransactionsService {

  private static final Logger log = LoggerFactory.getLogger(TransactionsServiceImpl.class);
  private final TransactionsRepository repository;
  private final TransactionsMapper mapper;

  public TransactionsServiceImpl(TransactionsRepository repository, TransactionsMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  /**
   * Creates a new transaction in the system.
   *
   * <p>This method processes and persists a new investment transaction based on the
   * provided request data. The transaction will be associated with a specific portfolio and asset,
   * recording details such as operation type, quantity, prices, and fees.
   *
   * @param transactionRequest the request object containing all transaction details including
   *                           brokerId, portfolioId, assetId, operationDate, operationType,
   *                           quantity, unitPrice, and fees; must not be null
   * @throws IllegalArgumentException if transactionRequest is null or contains invalid data
   * @throws RuntimeException         if an unexpected error occurs during transaction creation
   * @see TransactionRequest
   */
  @Override
  @Transactional
  public TransactionResponse createTransactions(@NotNull TransactionRequest transactionRequest) {
    TransactionEntity entity = mapper.mapToEntity(transactionRequest);
    entity = repository.save(entity);

    log.atInfo()
        .addKeyValue("transaction.id", entity.getId())
        .addKeyValue("transaction.portfolioId", entity.getPortfolioId())
        .addKeyValue("transaction.operationType", entity.getOperationType())
        .setMessage("Transaction created.")
        .log();

    return mapper.mapToResponse(entity);
  }

  /**
   * Deletes a transaction identified by the specified unique identifier.
   *
   * <p>This method permanently removes the transaction from the system. The operation
   * is idempotent - attempting to delete a non-existent transaction may not throw an exception.
   *
   * @param transactionId the unique identifier of the transaction to delete; must not be null
   * @throws IllegalArgumentException if id is null
   * @throws RuntimeException         if an error occurs during deletion
   */
  @Override
  public void deleteTransactions(@NotNull UUID transactionId) {
    if (!repository.existsById(transactionId)) {
      log.atWarn()
          .setMessage("Delete requested but transactionId {} not found")
          .addArgument(transactionId)
          .log();
      throw new TransactionNotFoundException("Transaction with id " + transactionId + " not found");
    }

    repository.deleteById(transactionId);
    log.atInfo()
        .setMessage("Transaction deleted successfully with transactionId {}")
        .addArgument(transactionId)
        .log();
  }

  /**
   * Retrieves a paginated and filtered list of transactions for a specific portfolio.
   *
   * <p>This method returns transactions matching the specified criteria. All filter
   * parameters except portfolioId are optional. Results can be filtered by asset, date range, and
   * operation type. The response includes pagination metadata.
   *
   * @param portfolioId   the unique identifier of the portfolio; must not be null or empty
   * @param assetId       the asset identifier to filter by; can be null to include all assets
   * @param fromDate      the start date for filtering transactions (inclusive); can be null to
   *                      include transactions from the beginning
   * @param toDate        the end date for filtering transactions (inclusive); can be null to
   *                      include transactions until the present
   * @param operationType the type of operation to filter by (e.g., BUY, SELL); can be null to
   *                      include all operation types
   * @param pageRequest   the pagination parameters
   * @return a {@link TransactionPageResponse} containing the paginated transaction data and
   * metadata; never null, but may contain an empty list if no transactions match
   * @throws IllegalArgumentException if portfolioId is null or empty
   * @throws RuntimeException         if an error occurs while retrieving transactions
   * @see TransactionPageResponse
   * @see OperationType
   */
  @Override
  public TransactionPageResponse getTransactions(String portfolioId, String assetId,
      LocalDate fromDate, LocalDate toDate, OperationType operationType, PageRequest pageRequest) {
    Specification<TransactionEntity> spec = TransactionSpecification.withFilters(
        portfolioId, assetId, fromDate, toDate, operationType
    );

    Page<TransactionEntity> page = repository.findAll(spec, pageRequest);

    log.atInfo()
        .setMessage("{} transactions fetched")
        .addArgument(page.getNumberOfElements())
        .addKeyValue("page", page.getNumber())
        .addKeyValue("size", page.getSize())
        .addKeyValue("totalElements", page.getTotalElements())
        .addKeyValue("totalPages", page.getTotalPages())
        .addKeyValue("returnedElements", page.getNumberOfElements())
        .log();

    return mapper.mapToPageResponse(page);
  }
}
