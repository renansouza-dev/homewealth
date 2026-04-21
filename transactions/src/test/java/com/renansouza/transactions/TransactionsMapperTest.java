package com.renansouza.transactions;

import com.renansouza.transactions.domain.TransactionEntity;
import com.renansouza.transactions.domain.TransactionsCalculator;
import com.renansouza.transactions.domain.TransactionsMapper;

import static java.time.temporal.ChronoUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.homewealth.transactions.model.OperationType;
import com.homewealth.transactions.model.TransactionPageResponse;
import com.homewealth.transactions.model.TransactionPageResponsePageable;
import com.homewealth.transactions.model.TransactionRequest;
import com.homewealth.transactions.model.TransactionResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;

@ExtendWith(MockitoExtension.class)
class TransactionsMapperTest {

  @Mock
  private TransactionsCalculator calculator;

  private TransactionsMapper mapper;

  private static final BigDecimal FEE_RATE = new BigDecimal("0.000275");

  @BeforeEach
  void setUp() {
    mapper = Mappers.getMapper(TransactionsMapper.class);
    mapper.calculator = calculator;
  }

  @ParameterizedTest
  @EnumSource(OperationType.class)
  void testMapFromTransactionRequest(OperationType operationType) {
    int quantity = 10;
    BigDecimal unitPrice = BigDecimal.TEN;
    BigDecimal gross = unitPrice.multiply(BigDecimal.valueOf(quantity));
    BigDecimal fees = gross.multiply(FEE_RATE).setScale(2, RoundingMode.HALF_UP);

    TransactionRequest request = new TransactionRequest();
    request.setBrokerId(UUID.randomUUID());
    request.setPortfolioId(UUID.randomUUID());
    request.setAssetId("ASSE4");
    request.setOperationDate(LocalDate.now());
    request.setQuantity(quantity);
    request.setUnitPrice(unitPrice);
    request.setOperationType(operationType);

    when(calculator.calculateFees(unitPrice, quantity)).thenReturn(fees);

    TransactionEntity entity = mapper.mapToEntity(request);

    LocalDateTime other = LocalDateTime.now().truncatedTo(MINUTES);
    assertAll(
        "Grouped Assertions of Transaction",
        () -> assertThat(entity).isNotNull(),
        () -> assertThat(entity.getId()).isNull(),
        () -> assertThat(entity.getAssetId()).isNotNull().isEqualTo(request.getAssetId()),
        () -> assertThat(entity.getFees()).isNotNull().isEqualTo(BigDecimal.valueOf(0.03)),
        () -> assertThat(entity.getBrokerId()).isNotNull().isEqualTo(request.getBrokerId()),
        () -> assertThat(entity.getUnitPrice()).isNotNull().isEqualTo(request.getUnitPrice()),
        () -> assertThat(entity.getCreatedAt().truncatedTo(MINUTES)).isNotNull().isEqualTo(other),
        () -> assertThat(entity.getPortfolioId()).isNotNull().isEqualTo(request.getPortfolioId()),
        () -> assertThat(entity.getOperationDate()).isNotNull()
            .isEqualTo(request.getOperationDate()),
        () -> assertThat(entity.getOperationType()).isNotNull()
            .isEqualTo(request.getOperationType()),
        () -> assertThat(entity.getQuantity()).isNotNull()
            .isEqualTo(request.getQuantity().intValue())
    );
    verify(calculator, atMostOnce()).calculateGross(any(), anyInt());
    verify(calculator, atMostOnce()).calculateNet(any(), anyInt(), any(), any());
  }

  @ParameterizedTest
  @EnumSource(OperationType.class)
  void testMapToResponse(OperationType operationType) {
    int quantity = 10;
    BigDecimal unitPrice = BigDecimal.TEN;
    BigDecimal gross = unitPrice.multiply(BigDecimal.valueOf(quantity));
    BigDecimal fees = gross.multiply(FEE_RATE).setScale(2, RoundingMode.HALF_UP);

    TransactionEntity entity = new TransactionEntity();
    entity.setFees(fees);
    entity.setAssetId("ASSE4");
    entity.setQuantity(quantity);
    entity.setUnitPrice(unitPrice);
    entity.setId(UUID.randomUUID());
    entity.setBrokerId(UUID.randomUUID());
    entity.setOperationType(operationType);
    entity.setPortfolioId(UUID.randomUUID());
    entity.setOperationDate(LocalDate.now());

    when(calculator.calculateNet(unitPrice, quantity, fees, operationType))
        .thenReturn(operationType == OperationType.BUY ? gross.add(fees) : gross.subtract(fees));

    TransactionResponse response = mapper.mapToResponse(entity);

    BigDecimal grossValue = entity.getUnitPrice()
        .multiply(BigDecimal.valueOf(entity.getQuantity()));
    BigDecimal netValue = entity.getOperationType() == OperationType.BUY
        ? grossValue.add(entity.getFees())
        : grossValue.subtract(entity.getFees());

    assertAll(
        "Grouped Assertions of Transaction",
        () -> assertThat(response).isNotNull(),
        () -> assertThat(response.getFees()).isNotNull().isEqualTo(fees),
        () -> assertThat(response.getNetValue()).isNotNull().isEqualTo(netValue),
        () -> assertThat(response.getId()).isNotNull().isExactlyInstanceOf(UUID.class),
        () -> assertThat(response.getAssetId()).isNotNull().isEqualTo(entity.getAssetId()),
        () -> assertThat(response.getBrokerId()).isNotNull().isEqualTo(entity.getBrokerId()),
        () -> assertThat(response.getUnitPrice()).isNotNull().isEqualTo(entity.getUnitPrice()),
        () -> assertThat(response.getPortfolioId()).isNotNull().isEqualTo(entity.getPortfolioId()),
        () -> assertThat(response.getOperationDate()).isNotNull()
            .isEqualTo(entity.getOperationDate()),
        () -> assertThat(response.getOperationType()).isNotNull()
            .isEqualTo(entity.getOperationType()),
        () -> assertThat(response.getQuantity().intValue()).isNotNull()
            .isEqualTo(entity.getQuantity())
    );
  }

  @Test
  void testMapToPageResponse() {
    TransactionPageResponse pageResponse = mapper.mapToPageResponse(Page.empty());

    assertAll(
        "Grouped Assertions of TransactionPageResponse",
        () -> assertThat(pageResponse).isNotNull(),
        () -> assertThat(pageResponse.getContent()).isNotNull().isEmpty(),
        () -> assertThat(pageResponse.getPageable()).isNotNull().isExactlyInstanceOf(
            TransactionPageResponsePageable.class),
        () -> assertThat(pageResponse.getPageable().getPageNumber()).isZero(),
        () -> assertThat(pageResponse.getPageable().getPageSize()).isZero(),
        () -> assertThat(pageResponse.getPageable().getTotalElements()).isZero()
    );
  }

  @Test
  void failedToMapToEntity() {
    TransactionEntity entity = mapper.mapToEntity(null);

    assertThat(entity).isNull();
  }

  @Test
  void failedToPapToResponse() {
    TransactionResponse response = mapper.mapToResponse(null);

    assertThat(response).isNull();
  }

}