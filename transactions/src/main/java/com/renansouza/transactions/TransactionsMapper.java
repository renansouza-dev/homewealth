package com.renansouza.transactions;

import com.homewealth.transactions.model.TransactionPageResponse;
import com.homewealth.transactions.model.TransactionPageResponsePageable;
import com.homewealth.transactions.model.TransactionRequest;
import com.homewealth.transactions.model.TransactionResponse;
import java.math.BigDecimal;
import java.util.List;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

/**
 * Mapper responsible for converting transaction-related objects between API layer models and
 * persistence entities.
 *
 * <p>
 * This mapper uses MapStruct to transform:
 * <ul>
 *     <li>{@link TransactionRequest} into {@link TransactionEntity}</li>
 *     <li>{@link TransactionEntity} into {@link TransactionResponse}</li>
 *     <li>{@link org.springframework.data.domain.Page} of entities into
 *         {@link TransactionPageResponse}</li>
 * </ul>
 * </p>
 *
 * <p>
 * In addition to object mapping, this mapper also performs transactional
 * financial calculations such as:
 * <ul>
 *     <li>Transaction fee calculation</li>
 *     <li>Gross value computation</li>
 *     <li>Net value calculation based on operation type</li>
 * </ul>
 * </p>
 *
 * <p>
 * Net value rules:
 * <ul>
 *     <li><b>BUY</b>: gross value + fees</li>
 *     <li><b>SELL</b>: gross value - fees</li>
 * </ul>
 * </p>
 *
 * <p>
 * This component centralizes transformation logic between layers,
 * ensuring consistency of financial calculations and API responses.
 * </p>
 *
 * @author Renan Alberto de Souza
 * @since 1.0
 */
@Mapper(componentModel = "spring", uses = TransactionsCalculator.class)
public abstract class TransactionsMapper {

  @Autowired
  protected TransactionsCalculator calculator;

  /**
   * Maps a transaction request received from the API layer into a {@link TransactionEntity}.
   *
   * <p>
   * During the mapping process, transaction fees are automatically calculated based on unit price
   * and quantity.
   * </p>
   *
   * @param request the incoming transaction request
   * @return a populated transaction entity ready for persistence
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "fees", ignore = true)
  public abstract TransactionEntity mapToEntity(TransactionRequest request);

  @AfterMapping
  protected void afterMapToEntity(TransactionRequest request, @MappingTarget TransactionEntity entity) {
    BigDecimal fees = calculator.calculateFees(request.getUnitPrice(), request.getQuantity());
    entity.setFees(fees);
  }

  /**
   * Converts a {@link TransactionEntity} into a {@link TransactionResponse} returned to API
   * consumers.
   *
   * @param entity the persisted transaction entity
   * @return the API response representation of the transaction
   */
  @Mapping(target = "netValue", ignore = true)
  public abstract TransactionResponse mapToResponse(TransactionEntity entity);

  @AfterMapping
  protected void afterMapToResponse(TransactionEntity entity, @MappingTarget TransactionResponse response) {
    BigDecimal netValue = calculator.calculateNet(entity.getUnitPrice(), entity.getQuantity(), entity.getFees(), entity.getOperationType());
    response.setNetValue(netValue);
  }

  /**
   * Converts a {@link List<TransactionEntity>} into a {@link List<TransactionResponse>} returned to
   * API consumers.
   *
   * @param entities the persisted transaction entity list
   * @return the API response representation of the transaction
   */
  public abstract List<TransactionResponse> map(List<TransactionEntity> entities);

  /**
   * Converts a paginated list of {@link TransactionEntity} objects into a
   * {@link TransactionPageResponse}.
   *
   * <p>
   * Pagination metadata such as page number, page size and total number of elements are preserved.
   * </p>
   *
   * @param page the paginated transaction entities
   * @return a paginated response containing mapped transactions
   */
  public TransactionPageResponse mapToPageResponse(Page<TransactionEntity> page) {
    TransactionPageResponsePageable pageable = new TransactionPageResponsePageable();
    pageable.setPageNumber(page.getNumber());
    pageable.setPageSize(page.getSize());
    pageable.setTotalElements(page.getTotalElements());

    TransactionPageResponse response = new TransactionPageResponse();
    response.setContent(map(page.getContent()));
    response.setPageable(pageable);

    return response;
  }

}