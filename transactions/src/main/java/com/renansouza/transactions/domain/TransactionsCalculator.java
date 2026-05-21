package com.renansouza.transactions.domain;

import com.homewealth.transactions.model.OperationType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Spring component responsible for performing financial calculations on transactions.
 *
 * <p>Provides methods to compute the gross value, fee, and net value of a transaction,
 * taking into account the operation type (BUY or SELL) and a configurable fee rate.
 *
 * <p>The fee rate is injected from the application property {@code transactions.fee-rate},
 * defaulting to {@code 0.000275} (0.0275%) if the property is not set.
 *
 * <p>Calculation summary:
 * <ul>
 *   <li><b>Gross</b> — {@code unitPrice * quantity}</li>
 *   <li><b>Fee</b> — {@code gross * feeRate}, rounded to 2 decimal places (HALF_UP)</li>
 *   <li><b>Net (BUY)</b> — {@code gross + fee}</li>
 *   <li><b>Net (SELL)</b> — {@code gross - fee}</li>
 * </ul>
 *
 * @see OperationType
 */
@Component
public class TransactionsCalculator {

  private final BigDecimal feeRate;

  public TransactionsCalculator(@Value("${transactions.fee-rate:0.000275}") BigDecimal feeRate) {
    this.feeRate = feeRate;
  }

  /**
   * Calculates the gross transaction value.
   *
   * <p>
   * Gross value calculation:
   * <ul>
   *     <li>Gross  -> unit price * quantity</li>
   * </ul>
   * </p>
   *
   * @param unitPrice   unit price from transaction
   * @param quantity    quantity from transaction
   * @return grossValue transaction value
   */
  public BigDecimal calculateGross(BigDecimal unitPrice, int quantity) {
    return unitPrice.multiply(BigDecimal.valueOf(quantity));
  }

  /**
   * Calculates the transaction fee based on the gross transaction value.
   *
   * <p>
   * Fee formula:
   * <pre>
   * fee = unitPrice * quantity * feeRate
   * </pre>
   * </p>
   *
   * @param unitPrice transaction unit price
   * @param quantity  transaction quantity
   * @return fee      transaction fee
   */
  public BigDecimal calculateFees(BigDecimal unitPrice, int quantity) {
    BigDecimal gross = calculateGross(unitPrice, quantity);
    return gross.multiply(feeRate).setScale(2, RoundingMode.HALF_UP);
  }

  /**
   * Calculates the net transaction value.
   *
   * <p>
   * Net value calculation:
   * <ul>
   *     <li>BUY  -> gross value + fees</li>
   *     <li>SELL -> gross value - fees</li>
   * </ul>
   * </p>
   *
   * @param unitPrice     transaction unit price
   * @param quantity      transaction quantity
   * @param fees          transaction fees value
   * @param operationType transaction operationType
   * @return netValue     transaction value
   */
  public BigDecimal calculateNet(BigDecimal unitPrice, int quantity, BigDecimal fees, OperationType operationType) {
    BigDecimal gross = calculateGross(unitPrice, quantity);
    return operationType == OperationType.BUY ? gross.add(fees) : gross.subtract(fees);
  }
}