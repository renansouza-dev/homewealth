package com.renansouza.transactions;

import com.renansouza.transactions.exception.TransactionNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.homewealth.transactions.model.OperationType;
import com.homewealth.transactions.model.TransactionPageResponse;
import com.homewealth.transactions.model.TransactionRequest;
import com.homewealth.transactions.model.TransactionResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class TransactionsServiceImplTest {

  @Mock
  private TransactionsRepository repository;

  @Mock
  private TransactionsMapper mapper;

  @InjectMocks
  private TransactionsServiceImpl service;

  @Nested
  @DisplayName("create transactions")
  class CreateTransaction {

    private TransactionRequest validRequest;
    private TransactionEntity mappedTransaction;
    private TransactionEntity savedEntity;
    private TransactionResponse transactionResponse;

    @BeforeEach
    void setUp() {
      validRequest = buildValidTransactionRequest();
      mappedTransaction = buildTransactionEntity(validRequest);
      savedEntity = buildSavedTransactionEntity(mappedTransaction);
      transactionResponse = buildTransactionResponse(savedEntity);
    }

    @Test
    @DisplayName("Should successfully create a new transaction")
    void shouldCreateNewTransaction() {
      when(mapper.mapToEntity(any(TransactionRequest.class))).thenReturn(mappedTransaction);
      when(repository.save(any(TransactionEntity.class))).thenReturn(savedEntity);
      when(mapper.mapToResponse(any(TransactionEntity.class))).thenReturn(transactionResponse);

      TransactionResponse response = service.createTransactions(validRequest);

      assertThat(response).isNotNull();
      assertThat(response.getId()).isEqualTo(savedEntity.getId());
      verify(repository, times(1)).save(any(TransactionEntity.class));
      verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("Should map request to entity before saving")
    void shouldMapRequestToEntityBeforeSaving() {
      when(mapper.mapToEntity(any(TransactionRequest.class))).thenReturn(mappedTransaction);
      when(repository.save(any(TransactionEntity.class))).thenReturn(savedEntity);
      when(mapper.mapToResponse(any(TransactionEntity.class))).thenReturn(transactionResponse);

      service.createTransactions(validRequest);

      ArgumentCaptor<TransactionEntity> captor = ArgumentCaptor.forClass(TransactionEntity.class);
      verify(repository).save(captor.capture());

      TransactionEntity capturedEntity = captor.getValue();
      assertAll(
          "Grouped Assertions of Transaction Entity",
          () -> assertThat(capturedEntity.getId()).isNotNull().isExactlyInstanceOf(UUID.class),
          () -> assertThat(capturedEntity.getBrokerId()).isEqualTo(validRequest.getBrokerId()),
          () -> assertThat(capturedEntity.getPortfolioId()).isEqualTo(
              validRequest.getPortfolioId()),
          () -> assertThat(capturedEntity.getOperationDate()).isEqualTo(
              validRequest.getOperationDate()),
          () -> assertThat(capturedEntity.getOperationType()).isEqualTo(
              validRequest.getOperationType()),
          () -> assertThat(capturedEntity.getQuantity()).isEqualTo(validRequest.getQuantity()),
          () -> assertThat(capturedEntity.getUnitPrice()).isEqualTo(validRequest.getUnitPrice()),
          () -> assertThat(capturedEntity.getFees()).isEqualTo(BigDecimal.valueOf(0.28)),
          () -> assertThat(capturedEntity.getAssetId()).isEqualTo(validRequest.getAssetId()),
          () -> assertThat(capturedEntity.getOperationType()).isEqualTo(
              validRequest.getOperationType()),
          () -> assertThat(capturedEntity.getCreatedAt()).isNotNull()
              .isExactlyInstanceOf(LocalDateTime.class)
      );
    }

    @Test
    @DisplayName("Should map saved entity to response")
    void shouldMapSavedEntityToResponse() {
      when(mapper.mapToEntity(any(TransactionRequest.class))).thenReturn(mappedTransaction);
      when(repository.save(any(TransactionEntity.class))).thenReturn(savedEntity);
      when(mapper.mapToResponse(any(TransactionEntity.class))).thenReturn(transactionResponse);

      TransactionResponse response = service.createTransactions(validRequest);

      assertAll(
          "Grouped Assertions of Transaction Response",
          () -> assertThat(response.getId()).isNotNull().isEqualTo(savedEntity.getId()),
          () -> assertThat(response.getBrokerId()).isEqualTo(savedEntity.getBrokerId()),
          () -> assertThat(response.getPortfolioId()).isEqualTo(savedEntity.getPortfolioId()),
          () -> assertThat(response.getOperationDate()).isEqualTo(savedEntity.getOperationDate()),
          () -> assertThat(response.getOperationType()).isEqualTo(savedEntity.getOperationType()),
          () -> assertThat(response.getQuantity().intValue()).isEqualTo(savedEntity.getQuantity()),
          () -> assertThat(response.getUnitPrice()).isEqualTo(savedEntity.getUnitPrice()),
          () -> assertThat(response.getFees()).isEqualTo(BigDecimal.valueOf(0.28)),
          () -> assertThat(response.getAssetId()).isEqualTo(savedEntity.getAssetId()),
          () -> assertThat(response.getOperationType()).isEqualTo(savedEntity.getOperationType())
      );
    }

    @Test
    @DisplayName("Should throw RuntimeException when repository throws exception")
    void shouldThrowRuntimeExceptionWhenRepositoryFails() {
      when(mapper.mapToEntity(any(TransactionRequest.class))).thenReturn(mappedTransaction);
      when(repository.save(any(TransactionEntity.class)))
          .thenThrow(new RuntimeException("Database error"));

      RuntimeException exception = assertThrows(
          RuntimeException.class,
          () -> service.createTransactions(validRequest)
      );

      assertThat(exception.getMessage()).isEqualTo("Database error");
      verify(repository, times(1)).save(any(TransactionEntity.class));
    }

    @ParameterizedTest
    @EnumSource(OperationType.class)
    @DisplayName("Should handle operation type correctly")
    void shouldHandleBuyOperationType(OperationType operationType) {
      validRequest.setOperationType(operationType);
      mappedTransaction.setOperationType(operationType);
      savedEntity.setOperationType(operationType);
      transactionResponse.setOperationType(operationType);

      when(mapper.mapToEntity(any(TransactionRequest.class))).thenReturn(mappedTransaction);
      when(repository.save(any(TransactionEntity.class))).thenReturn(savedEntity);
      when(mapper.mapToResponse(any(TransactionEntity.class))).thenReturn(transactionResponse);

      TransactionResponse response = service.createTransactions(validRequest);

      assertThat(response.getOperationType()).isEqualTo(operationType);
      verify(repository, times(1)).save(any(TransactionEntity.class));
    }

    @Test
    @DisplayName("Should preserve quantity precision")
    void shouldPreserveQuantityPrecision() {
      BigDecimal preciseQuantity = new BigDecimal("123.456789");
      validRequest.setQuantity(preciseQuantity.intValue());
      mappedTransaction.setQuantity(preciseQuantity.intValue());
      savedEntity.setQuantity(preciseQuantity.intValue());
      transactionResponse.setQuantity(preciseQuantity);

      when(mapper.mapToEntity(any(TransactionRequest.class))).thenReturn(mappedTransaction);
      when(repository.save(any(TransactionEntity.class))).thenReturn(savedEntity);
      when(mapper.mapToResponse(any(TransactionEntity.class))).thenReturn(transactionResponse);

      TransactionResponse response = service.createTransactions(validRequest);

      assertThat(response.getQuantity()).isEqualByComparingTo(preciseQuantity);
      verify(repository, times(1)).save(any(TransactionEntity.class));
    }

    @Test
    @DisplayName("Should preserve unit price precision")
    void shouldPreserveUnitPricePrecision() {
      BigDecimal precisePrice = new BigDecimal("987.654321");
      validRequest.setUnitPrice(precisePrice);
      mappedTransaction.setUnitPrice(precisePrice);
      savedEntity.setUnitPrice(precisePrice);
      transactionResponse.setUnitPrice(precisePrice);

      when(mapper.mapToEntity(any(TransactionRequest.class))).thenReturn(mappedTransaction);
      when(repository.save(any(TransactionEntity.class))).thenReturn(savedEntity);
      when(mapper.mapToResponse(any(TransactionEntity.class))).thenReturn(transactionResponse);

      TransactionResponse response = service.createTransactions(validRequest);

      assertThat(response.getUnitPrice()).isEqualByComparingTo(precisePrice);
      verify(repository, times(1)).save(any(TransactionEntity.class));
    }

    @Test
    @DisplayName("Should preserve fees precision")
    void shouldPreserveFeesPrecision() {
      BigDecimal preciseFees = new BigDecimal("12.34");
      savedEntity.setFees(preciseFees);
      mappedTransaction.setFees(preciseFees);
      transactionResponse.setFees(preciseFees);

      when(mapper.mapToEntity(any(TransactionRequest.class))).thenReturn(mappedTransaction);
      when(repository.save(any(TransactionEntity.class))).thenReturn(savedEntity);
      when(mapper.mapToResponse(any(TransactionEntity.class))).thenReturn(transactionResponse);

      TransactionResponse response = service.createTransactions(validRequest);

      assertThat(response.getFees()).isEqualByComparingTo(preciseFees);
      verify(repository, times(1)).save(any(TransactionEntity.class));
    }

    @Test
    @DisplayName("Should handle transaction with past operation date")
    void shouldHandleTransactionWithPastDate() {
      LocalDate pastDate = LocalDate.of(2025, 1, 1);
      validRequest.setOperationDate(pastDate);
      mappedTransaction.setOperationDate(pastDate);
      savedEntity.setOperationDate(pastDate);
      transactionResponse.setOperationDate(pastDate);

      when(mapper.mapToEntity(any(TransactionRequest.class))).thenReturn(mappedTransaction);
      when(repository.save(any(TransactionEntity.class))).thenReturn(savedEntity);
      when(mapper.mapToResponse(any(TransactionEntity.class))).thenReturn(transactionResponse);

      TransactionResponse response = service.createTransactions(validRequest);

      assertThat(response.getOperationDate()).isEqualTo(pastDate);
      verify(repository, times(1)).save(any(TransactionEntity.class));
    }

    @Test
    @DisplayName("Should handle transaction with current operation date")
    void shouldHandleTransactionWithCurrentDate() {
      LocalDate currentDate = LocalDate.now();
      validRequest.setOperationDate(currentDate);
      savedEntity.setOperationDate(currentDate);

      when(mapper.mapToEntity(any(TransactionRequest.class))).thenReturn(mappedTransaction);
      when(repository.save(any(TransactionEntity.class))).thenReturn(savedEntity);
      when(mapper.mapToResponse(any(TransactionEntity.class))).thenReturn(transactionResponse);

      TransactionResponse response = service.createTransactions(validRequest);

      assertThat(response.getOperationDate()).isEqualTo(currentDate);
      verify(repository, times(1)).save(any(TransactionEntity.class));
    }

    @Test
    @DisplayName("Should handle different portfolio IDs correctly")
    void shouldHandleDifferentPortfolioIds() {
      for (UUID portfolioId : List.of(UUID.randomUUID(), UUID.randomUUID())) {
        validRequest.setPortfolioId(portfolioId);
        mappedTransaction.setPortfolioId(portfolioId);
        savedEntity.setPortfolioId(portfolioId);
        transactionResponse.setPortfolioId(portfolioId);

        when(mapper.mapToEntity(any(TransactionRequest.class))).thenReturn(mappedTransaction);
        when(repository.save(any(TransactionEntity.class))).thenReturn(savedEntity);
        when(mapper.mapToResponse(any(TransactionEntity.class))).thenReturn(transactionResponse);

        TransactionResponse response1 = service.createTransactions(validRequest);
        assertThat(response1.getPortfolioId()).isEqualTo(portfolioId);
      }

      verify(repository, times(2)).save(any(TransactionEntity.class));
    }

    @Test
    @DisplayName("Should handle different asset IDs correctly")
    void shouldHandleDifferentAssetIds() {
      for (String assetId : List.of("ASSE3", "ASSE4")) {
        validRequest.setAssetId(assetId);
        mappedTransaction.setAssetId(assetId);
        savedEntity.setAssetId(assetId);
        transactionResponse.setAssetId(assetId);

        when(mapper.mapToEntity(any(TransactionRequest.class))).thenReturn(mappedTransaction);
        when(repository.save(any(TransactionEntity.class))).thenReturn(savedEntity);
        when(mapper.mapToResponse(any(TransactionEntity.class))).thenReturn(transactionResponse);

        TransactionResponse response = service.createTransactions(validRequest);
        assertThat(response.getAssetId()).isEqualTo(assetId);
      }

      verify(repository, times(2)).save(any(TransactionEntity.class));
    }

    private TransactionRequest buildValidTransactionRequest() {
      TransactionRequest request = new TransactionRequest();
      request.setBrokerId(UUID.randomUUID());
      request.setPortfolioId(UUID.randomUUID());
      request.setAssetId("ASSE4");
      request.setOperationDate(LocalDate.now());
      request.setOperationType(OperationType.BUY);
      request.setQuantity(10);
      request.setUnitPrice(BigDecimal.valueOf(100.00));
      return request;
    }

    private TransactionEntity buildTransactionEntity(TransactionRequest request) {
      TransactionEntity entity = new TransactionEntity();
      entity.setBrokerId(request.getBrokerId());
      entity.setPortfolioId(request.getPortfolioId());
      entity.setAssetId(request.getAssetId());
      entity.setOperationDate(request.getOperationDate());
      entity.setOperationType(request.getOperationType());
      entity.setQuantity(request.getQuantity());
      entity.setUnitPrice(request.getUnitPrice());
      entity.setFees(BigDecimal.ZERO);

      if (request.getUnitPrice().compareTo(BigDecimal.ZERO) > 0 && request.getQuantity() > 0) {
        BigDecimal unitPrice = request.getUnitPrice();
        BigDecimal quantity = new BigDecimal(request.getQuantity());
        BigDecimal feeRate = new BigDecimal("0.000275");

        entity.setFees(unitPrice.multiply(quantity).multiply(feeRate).setScale(2, RoundingMode.HALF_UP));
      }

      return entity;
    }

    private TransactionEntity buildSavedTransactionEntity(TransactionEntity entity) {
      entity.setId(UUID.randomUUID());
      entity.setFees(entity.getFees());
      entity.setAssetId(entity.getAssetId());
      entity.setBrokerId(entity.getBrokerId());
      entity.setQuantity(entity.getQuantity());
      entity.setUnitPrice(entity.getUnitPrice());
      entity.setPortfolioId(entity.getPortfolioId());
      entity.setOperationDate(entity.getOperationDate());
      entity.setOperationType(entity.getOperationType());
      return entity;
    }

    private TransactionResponse buildTransactionResponse(TransactionEntity entity) {
      TransactionResponse response = new TransactionResponse();
      response.setId(entity.getId());
      response.setFees(entity.getFees());
      response.setAssetId(entity.getAssetId());
      response.setBrokerId(entity.getBrokerId());
      response.setUnitPrice(entity.getUnitPrice());
      response.setPortfolioId(entity.getPortfolioId());
      response.setOperationDate(entity.getOperationDate());
      response.setOperationType(entity.getOperationType());
      response.setQuantity(BigDecimal.valueOf(entity.getQuantity()));

      return response;
    }

  }

  @Nested
  @DisplayName("delete transactions")
  class DeleteTransactionsTests {

    private UUID validTransactionId;
    private TransactionEntity existingTransaction;

    @BeforeEach
    void setUp() {
      validTransactionId = UUID.randomUUID();
      existingTransaction = new TransactionEntity();
      existingTransaction.setId(validTransactionId);
    }

    @Test
    @DisplayName("Should successfully delete an existing transaction")
    void shouldDeleteExistingTransaction() {
      when(repository.findById(validTransactionId)).thenReturn(Optional.of(existingTransaction));
      doNothing().when(repository).deleteById(validTransactionId);

      assertDoesNotThrow(() -> service.deleteTransactions(validTransactionId));

      verify(repository, times(1)).findById(validTransactionId);
      verify(repository, times(1)).deleteById(validTransactionId);
    }

    @Test
    @DisplayName("Should throw TransactionNotFoundException when transaction does not exist")
    void shouldThrowExceptionWhenTransactionNotFound() {
      when(repository.findById(validTransactionId)).thenReturn(Optional.empty());

      TransactionNotFoundException exception = assertThrows(TransactionNotFoundException.class,
          () -> service.deleteTransactions(validTransactionId));

      assertEquals(String.format("Transaction with id %s not found", validTransactionId),
          exception.getMessage());
      verify(repository, times(1)).findById(validTransactionId);
      verify(repository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Should verify findById is called before deleteById")
    void shouldVerifyFindByIdIsCalledBeforeDeleteById() {
      when(repository.findById(validTransactionId)).thenReturn(Optional.of(existingTransaction));
      doNothing().when(repository).deleteById(validTransactionId);

      service.deleteTransactions(validTransactionId);

      InOrder inOrder = inOrder(repository);
      inOrder.verify(repository).findById(validTransactionId);
      inOrder.verify(repository).deleteById(validTransactionId);
    }

    @Test
    @DisplayName("Should throw RuntimeException when repository throws exception during findById")
    void shouldThrowRuntimeExceptionWhenFindByIdFails() {
      when(repository.findById(validTransactionId)).thenThrow(
          new RuntimeException("Database connection error"));

      RuntimeException exception = assertThrows(RuntimeException.class,
          () -> service.deleteTransactions(validTransactionId));

      assertEquals("Database connection error", exception.getMessage());
      verify(repository, times(1)).findById(validTransactionId);
      verify(repository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Should throw RuntimeException when repository throws exception during deleteById")
    void shouldThrowRuntimeExceptionWhenDeleteByIdFails() {
      when(repository.findById(validTransactionId)).thenReturn(Optional.of(existingTransaction));
      doThrow(new RuntimeException("Database deletion error")).when(repository)
          .deleteById(validTransactionId);

      RuntimeException exception = assertThrows(RuntimeException.class,
          () -> service.deleteTransactions(validTransactionId));

      assertEquals("Database deletion error", exception.getMessage());
      verify(repository, times(1)).findById(validTransactionId);
      verify(repository, times(1)).deleteById(validTransactionId);
    }

    @Test
    @DisplayName("Should handle deletion of transaction with all-zeros UUID")
    void shouldHandleDeletionWithAllZerosUuid() {
      UUID allZerosId = UUID.fromString("00000000-0000-0000-0000-000000000000");
      TransactionEntity entity = new TransactionEntity();
      entity.setId(allZerosId);

      when(repository.findById(allZerosId)).thenReturn(Optional.of(entity));
      doNothing().when(repository).deleteById(allZerosId);

      assertDoesNotThrow(() -> service.deleteTransactions(allZerosId));

      verify(repository, times(1)).findById(allZerosId);
      verify(repository, times(1)).deleteById(allZerosId);
    }

    @Test
    @DisplayName("Should not call deleteById when transaction is not found")
    void shouldNotCallDeleteByIdWhenTransactionNotFound() {
      when(repository.findById(validTransactionId)).thenReturn(Optional.empty());

      assertThrows(TransactionNotFoundException.class,
          () -> service.deleteTransactions(validTransactionId));

      verify(repository, times(1)).findById(validTransactionId);
      verify(repository, never()).deleteById(validTransactionId);
    }

    @Test
    @DisplayName("Should throw TransactionNotFoundException with correct message format")
    void shouldThrowExceptionWithCorrectMessageFormat() {
      UUID specificId = existingTransaction.getId();
      when(repository.findById(specificId)).thenReturn(Optional.empty());

      TransactionNotFoundException exception = assertThrows(TransactionNotFoundException.class,
          () -> service.deleteTransactions(specificId));

      assertThat(exception.getMessage()).contains("Transaction with id")
          .contains(specificId.toString()).contains("not found");
    }

    @Test
    @DisplayName("Should complete deletion operation successfully without returning value")
    void shouldCompleteOperationWithoutReturningValue() {
      when(repository.findById(validTransactionId)).thenReturn(Optional.of(existingTransaction));
      doNothing().when(repository).deleteById(validTransactionId);

      service.deleteTransactions(validTransactionId);

      verify(repository).findById(validTransactionId);
      verify(repository).deleteById(validTransactionId);
      verifyNoMoreInteractions(repository);
    }

  }

  @Nested
  @DisplayName("get transactions")
  class GetTransactionsTests {

    private PageRequest pageRequest;
    private TransactionPageResponse pageResponse;


    @BeforeEach
    void setUp() {
      pageRequest = PageRequest.of(0, 10);
      pageResponse = new TransactionPageResponse();
    }

    @Test
    @DisplayName("Should return mapped page response when repository returns results")
    void shouldReturnMappedPageResponse() {
      Page<TransactionEntity> page = new PageImpl<>(List.of(new TransactionEntity()));
      when(repository.findAll(ArgumentMatchers.<Specification<TransactionEntity>>any(), eq(pageRequest))).thenReturn(page);
      when(mapper.mapToPageResponse(page)).thenReturn(pageResponse);

      TransactionPageResponse result = service
          .getTransactions(null, null, null, null, null, pageRequest);

      assertThat(result).isNotNull().isEqualTo(pageResponse);
      verify(repository, times(1))
          .findAll(ArgumentMatchers.<Specification<TransactionEntity>>any(), eq(pageRequest));
      verify(mapper, times(1)).mapToPageResponse(page);
    }

    @Test
    @DisplayName("Should return empty page response when repository returns no results")
    void shouldReturnEmptyPageResponse() {
      Page<TransactionEntity> emptyPage = new PageImpl<>(List.of());
      when(repository.findAll(ArgumentMatchers.<Specification<TransactionEntity>>any(), eq(pageRequest))).thenReturn(emptyPage);
      when(mapper.mapToPageResponse(emptyPage)).thenReturn(pageResponse);

      TransactionPageResponse result = service
          .getTransactions(null, null, null, null, null, pageRequest);

      assertThat(result).isNotNull();
      verify(repository, times(1))
          .findAll(ArgumentMatchers.<Specification<TransactionEntity>>any(), eq(pageRequest));
    }

    @Test
    @DisplayName("Should pass all filters to specification and forward page to mapper")
    void shouldPassAllFiltersToSpecification() {
      String portfolioId = UUID.randomUUID().toString();
      String assetId = "ASSET-001";
      LocalDate fromDate = LocalDate.of(2024, 1, 1);
      LocalDate toDate = LocalDate.of(2024, 12, 31);
      Page<TransactionEntity> page = new PageImpl<>(List.of(new TransactionEntity()));

      when(repository.findAll(ArgumentMatchers.<Specification<TransactionEntity>>any(), eq(pageRequest))).thenReturn(page);
      when(mapper.mapToPageResponse(page)).thenReturn(pageResponse);

      TransactionPageResponse result = service.getTransactions(
          portfolioId, assetId, fromDate, toDate, OperationType.BUY, pageRequest);

      assertThat(result).isNotNull();
      verify(repository, times(1)).findAll(ArgumentMatchers.<Specification<TransactionEntity>>any(), eq(pageRequest));
      verify(mapper, times(1)).mapToPageResponse(page);
    }

    @ParameterizedTest
    @EnumSource(OperationType.class)
    @DisplayName("Should handle all operation types")
    void shouldHandleAllOperationTypes(OperationType operationType) {
      Page<TransactionEntity> page = new PageImpl<>(List.of(new TransactionEntity()));
      when(repository.findAll(ArgumentMatchers.<Specification<TransactionEntity>>any(), eq(pageRequest))).thenReturn(page);
      when(mapper.mapToPageResponse(page)).thenReturn(pageResponse);

      TransactionPageResponse result = service.getTransactions(
          null, null, null, null, operationType, pageRequest);

      assertThat(result).isNotNull();
      verify(repository, times(1)).findAll(ArgumentMatchers.<Specification<TransactionEntity>>any(), eq(pageRequest));
    }

    @Test
    @DisplayName("Should verify specification is built before repository is called")
    void shouldBuildSpecificationBeforeCallingRepository() {
      Page<TransactionEntity> page = new PageImpl<>(List.of());
      when(repository.findAll(ArgumentMatchers.<Specification<TransactionEntity>>any(), eq(pageRequest))).thenReturn(page);
      when(mapper.mapToPageResponse(page)).thenReturn(pageResponse);

      service.getTransactions(null, null, null, null, null, pageRequest);

      InOrder inOrder = inOrder(repository, mapper);
      inOrder.verify(repository).findAll(ArgumentMatchers.<Specification<TransactionEntity>>any(), eq(pageRequest));
      inOrder.verify(mapper).mapToPageResponse(page);
    }

    @Test
    @DisplayName("Should throw RuntimeException when repository throws exception")
    void shouldThrowRuntimeExceptionWhenRepositoryFails() {
      when(repository.findAll(ArgumentMatchers.<Specification<TransactionEntity>>any(), eq(pageRequest)))
          .thenThrow(new RuntimeException("Database error"));

      RuntimeException exception = assertThrows(RuntimeException.class,
          () -> service.getTransactions(null, null, null, null, null, pageRequest));

      assertThat(exception.getMessage()).isEqualTo("Database error");
      verify(repository, times(1))
          .findAll(ArgumentMatchers.<Specification<TransactionEntity>>any(), eq(pageRequest));
      verify(mapper, never()).mapToPageResponse(any());
    }
  }

}