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

@RestController
@RequestMapping("/api/v1")
public class TransactionsController implements TransactionsApi {

  private final TransactionsService service;

  public TransactionsController(TransactionsService service) {
    this.service = service;
  }

  @Override
  public ResponseEntity<TransactionResponse> createTransactions(TransactionRequest transactionRequest) {
    TransactionResponse response = service.createTransactions(transactionRequest);
    URI location = URI.create("/transactions/" + response.getId());
    return ResponseEntity.created(location).body(response);
  }

  @Override
  public ResponseEntity<Void> deleteTransactions(UUID transactionId) {
    service.deleteTransactions(transactionId);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<TransactionPageResponse> getTransactions(String portfolioId, String assetId,
      LocalDate fromDate, LocalDate toDate, OperationType operationType, Integer page, Integer size,
      String sortBy, String sortOrder) {

    Sort sort = Sort.by(Direction.fromString(sortOrder), sortBy);
    PageRequest pageRequest = PageRequest.of(page, size, sort);

    return ResponseEntity.ok(service.getTransactions(portfolioId, assetId, fromDate, toDate, operationType, pageRequest));
  }

}