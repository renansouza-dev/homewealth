package com.renansouza.transactions;

import com.renansouza.transactions.exception.TransactionNotFoundException;
import com.renansouza.transactions.exception.TransactionsExceptionHandler;

import static com.homewealth.transactions.api.TransactionsApi.PATH_CREATE_TRANSACTIONS;
import static com.homewealth.transactions.api.TransactionsApi.PATH_DELETE_TRANSACTIONS;
import static com.homewealth.transactions.api.TransactionsApi.PATH_GET_TRANSACTIONS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.Is.isA;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.homewealth.transactions.model.OperationType;
import com.homewealth.transactions.model.TransactionPageResponse;
import com.homewealth.transactions.model.TransactionPageResponsePageable;
import com.homewealth.transactions.model.TransactionRequest;
import com.homewealth.transactions.model.TransactionResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.verification.VerificationMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

@Tag("unit")
@WebMvcTest(TransactionsController.class)
@Import(TransactionsExceptionHandler.class)
@DisplayName("TransactionsController Tests")
class TransactionsControllerTest {

  private static final String CONTEXT_PATH = "/api/v1/";
  private static final UUID PORTFOLIO_ID = UUID.randomUUID();
  private static final UUID BROKER_ID = UUID.randomUUID();
  private static final String ASSET_ID = "ASSE4";
  private static final VerificationMode ONCE = Mockito.times(1);

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private TransactionsService service;

  @Nested
  @DisplayName("DELETE /api/v1/transactions/{transactionId}")
  class DeleteTransactionsTests {

    @Test
    @DisplayName("Should return 204 No Content when transaction is successfully deleted")
    void shouldReturn204WhenTransactionIsDeleted() throws Exception {
      UUID validTransactionId = UUID.randomUUID();
      doNothing().when(service).deleteTransactions(validTransactionId);

      MvcResult result = mockMvc.perform(
              delete(CONTEXT_PATH + PATH_DELETE_TRANSACTIONS, validTransactionId))
          .andExpect(status().isNoContent())
          .andReturn();

      assertAll(
          () -> assertThat(result.getResponse()).isNotNull(),
          () -> assertThat(result.getResponse().getContentAsString()).isEmpty(),
          () -> assertThat(result.getResponse().getContentLength()).isZero(),
          () -> assertThat(result.getRequest().getRequestURI())
              .isEqualTo(CONTEXT_PATH + "transactions/" + validTransactionId)
      );

      verify(service, ONCE).deleteTransactions(validTransactionId);
    }

    @Test
    @DisplayName("Should return 400 Bad Request when transaction ID is invalid UUID format")
    void shouldReturn400WhenInvalidUuidFormat() throws Exception {
      String invalidUuid = "invalid-uuid-format";

      mockMvc.perform(delete(CONTEXT_PATH + PATH_DELETE_TRANSACTIONS, invalidUuid))
          .andExpect(status().isBadRequest());

      verify(service, never()).deleteTransactions(any(UUID.class));
    }

    @Test
    @DisplayName("Should return 404 Not Found when transaction does not exist")
    void shouldReturn404WhenTransactionNotFound() throws Exception {
      UUID nonExistentId = UUID.randomUUID();
      doThrow(new TransactionNotFoundException("Transaction not found with ID: " + nonExistentId))
          .when(service).deleteTransactions(nonExistentId);

      MvcResult result = mockMvc.perform(
              delete(CONTEXT_PATH + PATH_DELETE_TRANSACTIONS, nonExistentId))
          .andExpect(status().isNotFound())
          .andReturn();

      verify(service, ONCE).deleteTransactions(nonExistentId);
      assertThat(result.getResponse().getContentAsString()).isNotEmpty();
    }

    @Test
    @DisplayName("Should return 500 Internal Server Error when service throws unexpected exception")
    void shouldReturn500WhenServiceFails() throws Exception {
      UUID validTransactionId = UUID.randomUUID();
      doThrow(new RuntimeException("Database connection error"))
          .when(service).deleteTransactions(validTransactionId);

      mockMvc.perform(delete(CONTEXT_PATH + PATH_DELETE_TRANSACTIONS, validTransactionId))
          .andExpect(status().isInternalServerError());

      verify(service, ONCE).deleteTransactions(validTransactionId);
    }

    @Test
    @DisplayName("Should handle all-zeros UUID correctly")
    void shouldHandleAllZerosUuid() throws Exception {
      UUID allZerosId = UUID.fromString("00000000-0000-0000-0000-000000000000");
      doThrow(new TransactionNotFoundException("Transaction not found"))
          .when(service).deleteTransactions(allZerosId);

      mockMvc.perform(delete(CONTEXT_PATH + PATH_DELETE_TRANSACTIONS, allZerosId))
          .andExpect(status().isNotFound());

      verify(service).deleteTransactions(allZerosId);
    }

  }

  @Nested
  @DisplayName("POST /api/v1/transactions")
  class CreateTransactionsTests {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    @DisplayName("Should return 201 CREATED with transaction response when transaction is successfully created")
    void shouldReturn200WhenTransactionIsCreated() throws Exception {
      when(service.createTransactions(any(TransactionRequest.class))).thenReturn(
          buildValidResponse());

      mockMvc.perform(post(CONTEXT_PATH + PATH_CREATE_TRANSACTIONS)
              .contentType(MediaType.APPLICATION_JSON)
              .content(MAPPER.writeValueAsString(buildValidRequest())))
          .andExpect(status().isCreated())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.fees", is(0.03)))
          .andExpect(jsonPath("$.id", isA(String.class)))
          .andExpect(jsonPath("$.assetId", is(ASSET_ID)))
          .andExpect(jsonPath("$.quantity", is(10)))
          .andExpect(jsonPath("$.unitPrice", is(10)))
          .andExpect(jsonPath("$.netValue", is(99.97)))
          .andExpect(jsonPath("$.brokerId", is(BROKER_ID.toString())))
          .andExpect(jsonPath("$.portfolioId", is(PORTFOLIO_ID.toString())))
          .andExpect(jsonPath("$.operationDate", is(LocalDate.now().toString())))
          .andExpect(jsonPath("$.operationType", is(OperationType.BUY.toString())));

      verify(service, ONCE).createTransactions(any(TransactionRequest.class));
    }

    @Test
    @DisplayName("Should return 400 BAD REQUEST when transaction request is mal formed")
    void shouldReturn400WhenTransactionIsMalformed() throws Exception {
      mockMvc.perform(post(CONTEXT_PATH + PATH_CREATE_TRANSACTIONS)
              .contentType(MediaType.APPLICATION_JSON)
              .content("{"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 405 METHOD NOT ALLOWED when transaction request is mal formed")
    void shouldReturn405WhenTransactionIsMalformed() throws Exception {
      mockMvc.perform(patch(CONTEXT_PATH + PATH_CREATE_TRANSACTIONS)
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isMethodNotAllowed())
          .andExpect(jsonPath("$.details", is("Request method 'PATCH' is not supported")))
          .andExpect(jsonPath("$.timestamp", containsString(LocalDate.now().toString())));
    }

    @Test
    @DisplayName("Should return 500 INTERNAL SERVER ERROR when something went wrong on the server side")
    void shouldReturn500WhenTransactionIsMalformed() throws Exception {
      when(service.createTransactions(any(TransactionRequest.class)))
          .thenThrow(new RuntimeException("unexpected exception"));

      mockMvc.perform(post(CONTEXT_PATH + PATH_CREATE_TRANSACTIONS)
              .content(MAPPER.writeValueAsString(buildValidRequest()))
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isInternalServerError())
          .andExpect(jsonPath("$.details", is("unexpected exception")))
          .andExpect(jsonPath("$.timestamp", containsString(LocalDate.now().toString())));
    }

    private TransactionRequest buildValidRequest() {
      TransactionRequest request = new TransactionRequest();
      request.setOperationDate(LocalDate.now());
      request.setOperationType(OperationType.BUY);
      request.setPortfolioId(PORTFOLIO_ID);
      request.setUnitPrice(BigDecimal.TEN);
      request.setBrokerId(BROKER_ID);
      request.setAssetId(ASSET_ID);
      request.setQuantity(1);

      return request;
    }

  }

  @Nested
  @DisplayName("GET /api/v1/transactions")
  class GetTransactionsTests {

    @Test
    @DisplayName("Should return 200 OK with paginated transactions")
    void shouldReturn200WithAllMandatoryQueryParameters() throws Exception {
      TransactionPageResponse response = buildPageResponse(List.of(buildValidResponse()));

      when(service.getTransactions(any(), any(), any(), any(), any(), any(PageRequest.class)))
          .thenReturn(response);
      mockMvc.perform(get(CONTEXT_PATH + PATH_GET_TRANSACTIONS))
          .andExpect(status().isOk())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.content", hasSize(1)))
          .andExpect(jsonPath("$.pageable.pageSize", is(20)))
          .andExpect(jsonPath("$.pageable.pageNumber", is(0)))
          .andExpect(jsonPath("$.pageable.totalElements", is(1)));

      verify(service, ONCE).getTransactions(any(), any(), any(), any(), any(), any(PageRequest.class));
    }

    @Test
    @DisplayName("Should return 500 Internal Server Error when service throws unexpected exception")
    void shouldReturn500WhenServiceFails() throws Exception {
      when(service.getTransactions(any(), any(), any(), any(), any(), any(PageRequest.class)))
          .thenThrow(new RuntimeException("Database connection error"));

      mockMvc.perform(get(CONTEXT_PATH + PATH_GET_TRANSACTIONS))
          .andExpect(status().isInternalServerError())
          .andExpect(jsonPath("$.details", is("Database connection error")))
          .andExpect(jsonPath("$.timestamp", containsString(LocalDate.now().toString())));

      verify(service, ONCE).getTransactions(any(), any(), any(), any(), any(), any(PageRequest.class));
    }

    private TransactionPageResponse buildPageResponse(List<TransactionResponse> content) {
      TransactionPageResponsePageable pageable = new TransactionPageResponsePageable();
      pageable.setTotalElements((long) content.size());
      pageable.setPageNumber(0);
      pageable.setPageSize(20);

      TransactionPageResponse response = new TransactionPageResponse();
      response.setContent(content);
      response.setPageable(pageable);

      return response;
    }

  }

  private TransactionResponse buildValidResponse() {
    TransactionResponse response = new TransactionResponse();
    response.setOperationDate(LocalDate.now());
    response.setOperationType(OperationType.BUY);
    response.setPortfolioId(PORTFOLIO_ID);
    response.setId(UUID.randomUUID());
    response.setBrokerId(BROKER_ID);
    response.setAssetId(ASSET_ID);
    response.setUnitPrice(BigDecimal.TEN);
    response.setQuantity(BigDecimal.TEN);

    BigDecimal feeRate = BigDecimal.valueOf(0.0275)
        .divide(BigDecimal.valueOf(100), RoundingMode.CEILING);

    response.setFees(response.getQuantity().multiply(response.getUnitPrice()).multiply(feeRate)
        .setScale(2, RoundingMode.CEILING));
    response.setNetValue(
        response.getQuantity().multiply(response.getUnitPrice()).subtract(response.getFees())
            .setScale(2, RoundingMode.CEILING));

    return response;
  }

}