package com.renansouza.transactions.service;

import com.homewealth.transactions.model.OperationType;
import com.homewealth.transactions.model.TransactionPageResponse;
import com.homewealth.transactions.model.TransactionRequest;
import com.homewealth.transactions.model.TransactionResponse;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;

/**
 * Service interface for managing financial transactions operations.
 *
 * <p>This interface defines the core operations for creating, retrieving, and deleting
 * transactions within the system. Implementations of this interface should handle the business
 * logic and persistence operations related to transaction management.
 *
 * @author Renan Alberto de Souza
 * @since 1.0
 */
public interface TransactionsService {

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
   * @return a {@link TransactionResponse} containing the transaction data. This is not an
   * idempotent operation.
   * @see TransactionRequest
   */
  TransactionResponse createTransactions(TransactionRequest transactionRequest);

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
  void deleteTransactions(UUID transactionId);

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
  TransactionPageResponse getTransactions(String portfolioId, String assetId, LocalDate fromDate,
      LocalDate toDate, OperationType operationType, PageRequest pageRequest);

}