package com.renansouza.transactions.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.homewealth.transactions.model.OperationType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class TransactionsCalculatorTest {

  private TransactionsCalculator calculator;
  private static final BigDecimal FEE_RATE = new BigDecimal("0.000275");

  @BeforeEach
  void setUp() {
    calculator = new TransactionsCalculator(FEE_RATE);
  }

  @ParameterizedTest
  @EnumSource(value = OperationType.class)
  void calculateNet(OperationType type) {
    int quantity = 10;
    BigDecimal unitPrice = BigDecimal.TEN;
    BigDecimal gross = unitPrice.multiply(BigDecimal.valueOf(quantity));
    BigDecimal fees = gross.multiply(FEE_RATE).setScale(2, RoundingMode.HALF_UP);

    assertThat(calculator.calculateNet(unitPrice, quantity, fees, type))
        .isEqualTo(type == OperationType.BUY ? gross.add(fees) : gross.subtract(fees));
  }

  @Test
  void calculateGross() {
    int quantity = 10;
    BigDecimal unitPrice = BigDecimal.TEN;

    assertThat(calculator.calculateGross(unitPrice, quantity))
        .isEqualTo(unitPrice.multiply(BigDecimal.valueOf(quantity)));
  }

  @Test
  void calculateFees() {
    int quantity = 10;
    BigDecimal unitPrice = BigDecimal.TEN;
    BigDecimal gross = unitPrice.multiply(BigDecimal.valueOf(quantity));

    assertThat(calculator.calculateFees(unitPrice, quantity))
        .isEqualTo(gross.multiply(FEE_RATE).setScale(2, RoundingMode.HALF_UP));
  }
}