package com.renansouza.transactions;

import com.homewealth.transactions.model.OperationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "transactions", schema = "public")
public class TransactionEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @JdbcTypeCode(SqlTypes.UUID)
  @Column(columnDefinition = "UUID DEFAULT uuidv7()", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "broker_id", nullable = false, updatable = false)
  private UUID brokerId;

  @Column(name = "portfolio_id", nullable = false, updatable = false)
  private UUID portfolioId;

  @Min(5)
  @Max(6)
  @Column(name = "asset_id", length = 20, nullable = false, updatable = false)
  private String assetId;

  @PastOrPresent
  @Column(name = "operation_date", nullable = false)
  private LocalDate operationDate;

  @Enumerated(EnumType.STRING)
  @Column(name = "operation_type", length = 10, nullable = false)
  private OperationType operationType;

  @PositiveOrZero
  @Column(nullable = false)
  private Integer quantity;

  @PositiveOrZero
  @Column(name = "unit_price", precision = 10, scale = 4, nullable = false)
  private BigDecimal unitPrice;

  @PositiveOrZero
  @Column(precision = 10, scale = 2, nullable = false)
  private BigDecimal fees;

  @NotNull
  @ColumnDefault("now()")
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  public TransactionEntity() {
  }

  public TransactionEntity(UUID id, UUID brokerId, UUID portfolioId, String assetId,
      LocalDate operationDate, OperationType operationType, Integer quantity, BigDecimal unitPrice,
      BigDecimal fees, LocalDateTime createdAt) {
    this.id = id;
    this.brokerId = brokerId;
    this.portfolioId = portfolioId;
    this.assetId = assetId;
    this.operationDate = operationDate;
    this.operationType = operationType;
    this.quantity = quantity;
    this.unitPrice = unitPrice;
    this.fees = fees;
    this.createdAt = createdAt;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getBrokerId() {
    return brokerId;
  }

  public void setBrokerId(UUID brokerId) {
    this.brokerId = brokerId;
  }

  public UUID getPortfolioId() {
    return portfolioId;
  }

  public void setPortfolioId(UUID portfolioId) {
    this.portfolioId = portfolioId;
  }

  public String getAssetId() {
    return assetId;
  }

  public void setAssetId(String assetId) {
    this.assetId = assetId;
  }

  public LocalDate getOperationDate() {
    return operationDate;
  }

  public void setOperationDate(LocalDate operationDate) {
    this.operationDate = operationDate;
  }

  public OperationType getOperationType() {
    return operationType;
  }

  public void setOperationType(OperationType operationType) {
    this.operationType = operationType;
  }

  public Integer getQuantity() {
    return quantity;
  }

  public void setQuantity(Integer quantity) {
    this.quantity = quantity;
  }

  public BigDecimal getUnitPrice() {
    return unitPrice;
  }

  public void setUnitPrice(BigDecimal unitPrice) {
    this.unitPrice = unitPrice;
  }

  public BigDecimal getFees() {
    return fees;
  }

  public void setFees(BigDecimal fees) {
    this.fees = fees;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

}