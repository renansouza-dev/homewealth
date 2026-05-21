package com.renansouza.transactions.api;

import com.renansouza.transactions.service.TransactionsService;

import com.homewealth.transactions.api.TransactionsApi;
import com.homewealth.transactions.model.OperationType;
import com.homewealth.transactions.model.TransactionPageResponse;
import com.homewealth.transactions.model.TransactionRequest;
import com.homewealth.transactions.model.TransactionResponse;
import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller that handles HTTP requests for transaction management.
 *
 * <p>Exposes endpoints under {@code /api/v1} and delegates all business logic
 * to {@link TransactionsService}. Implements the {@link TransactionsApi} contract,
 * which defines the available operations and their signatures.
 *
 * <p>Supported operations:
 * <ul>
 *   <li>Create a new transaction</li>
 *   <li>Delete an existing transaction by ID</li>
 *   <li>Retrieve a paginated and filtered list of transactions</li>
 * </ul>
 *
 * @see TransactionsApi
 * @see TransactionsService
 */
@RestController
@RequestMapping("/api/v1")
public class TransactionsController implements TransactionsApi {

  private final TransactionsService service;

  public TransactionsController(TransactionsService service) {
    this.service = service;
  }

  /**
   * Creates a new transaction based on the provided request data.
   *
   * @param transactionRequest the request body containing the transaction details
   * @return a {@link ResponseEntity} with HTTP 201 Created status, the {@code Location} header
   *         pointing to the created resource, and the created {@link TransactionResponse} as body
   */
  @Override
  public ResponseEntity<TransactionResponse> createTransactions(TransactionRequest transactionRequest) {
    TransactionResponse response = service.createTransactions(transactionRequest);
    URI location = URI.create("/transactions/" + response.getId());
    return ResponseEntity.created(location).body(response);
  }

  /**
   * Deletes the transaction identified by the given UUID.
   *
   * @param transactionId the unique identifier of the transaction to be deleted
   * @return a {@link ResponseEntity} with HTTP 204 No Content status and no body
   */
  @Override
  public ResponseEntity<Void> deleteTransactions(UUID transactionId) {
    service.deleteTransactions(transactionId);
    return ResponseEntity.noContent().build();
  }

  /**
   * Retrieves a paginated list of transactions filtered by the provided criteria.
   *
   * @param portfolioId   the identifier of the portfolio to filter by, or {@code null} for all portfolios
   * @param assetId       the identifier of the asset to filter by, or {@code null} for all assets
   * @param fromDate      the start date (inclusive) of the transaction date range, or {@code null} for no lower bound
   * @param toDate        the end date (inclusive) of the transaction date range, or {@code null} for no upper bound
   * @param operationType the type of operation to filter by (e.g. BUY, SELL), or {@code null} for all types
   * @param page          the zero-based page index to retrieve
   * @param size          the number of records per page
   * @param sortBy        the field name to sort the results by
   * @param sortOrder     the sort direction, either {@code "ASC"} or {@code "DESC"}
   * @return a {@link ResponseEntity} with HTTP 200 OK status and a {@link TransactionPageResponse} as body
   */
  @Override
  public ResponseEntity<TransactionPageResponse> getTransactions(String portfolioId, String assetId,
      LocalDate fromDate, LocalDate toDate, OperationType operationType, Integer page, Integer size,
      String sortBy, String sortOrder) {

    Sort sort = Sort.by(Direction.fromString(sortOrder), sortBy);
    PageRequest pageRequest = PageRequest.of(page, size, sort);

    return ResponseEntity.ok(service.getTransactions(portfolioId, assetId, fromDate, toDate, operationType, pageRequest));
  }

}