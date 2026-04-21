package com.renansouza.transactions;

import com.renansouza.transactions.domain.TransactionEntity;
import com.renansouza.transactions.repository.TransactionsRepository;

import static com.homewealth.transactions.api.TransactionsApi.PATH_CREATE_TRANSACTIONS;
import static com.homewealth.transactions.api.TransactionsApi.PATH_DELETE_TRANSACTIONS;
import static com.homewealth.transactions.api.TransactionsApi.PATH_GET_TRANSACTIONS;
import static io.restassured.RestAssured.given;
import static io.restassured.config.JsonConfig.jsonConfig;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import com.homewealth.transactions.model.OperationType;
import com.homewealth.transactions.model.TransactionRequest;
import io.restassured.RestAssured;
import io.restassured.config.LogConfig;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.path.json.config.JsonPathConfig.NumberReturnType;
import io.restassured.response.Response;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import tools.jackson.databind.json.JsonMapper;

@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TransactionsControllerIT {

  private static final String CONTEXT_PATH = "/api/v1";

  @LocalServerPort
  private Integer port;

  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18.3-alpine");

  @BeforeAll
  static void beforeAll() {
    postgres.start();
  }

  @AfterAll
  static void afterAll() {
    postgres.stop();
  }

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired
  TransactionsRepository repository;

  @BeforeEach
  void setUp() {
    RestAssured.baseURI = "http://localhost:" + port;
    RestAssured.config = RestAssured.config().jsonConfig(
        jsonConfig().numberReturnType(NumberReturnType.BIG_DECIMAL)
    );

    RestAssured.config = RestAssured.config().logConfig(
        LogConfig.logConfig().enableLoggingOfRequestAndResponseIfValidationFails(LogDetail.ALL)
    );

    repository.deleteAll();
  }

  @Nested
  @DisplayName("GET /api/v1/transactions")
  class GetTransactionsTests {

    @Test
    void shouldGetNoTransactions() {
      given()
          .contentType(ContentType.JSON)
          .when()
          .get(CONTEXT_PATH + PATH_GET_TRANSACTIONS)
          .then()
          .statusCode(200)
          .body("content", empty())
          .body("pageable.pageNumber", equalTo(0))
          .body("pageable.pageSize", equalTo(20))
          .body("pageable.totalElements", equalTo(0));
    }

    @Test
    void shouldGetOneTransactions() {
      TransactionEntity entity = getTransactionEntity();
      repository.save(entity);

      BigDecimal grossValue = entity.getUnitPrice().multiply(BigDecimal.TEN);
      BigDecimal netValue = grossValue.add(entity.getFees());

      given()
          .contentType(ContentType.JSON)
          .when()
          .get(CONTEXT_PATH + PATH_GET_TRANSACTIONS)
          .then()
          .statusCode(200)
          .body("content", hasSize(1))
          .body("content.[0].id", notNullValue())
          .body("content.[0].brokerId", equalTo(entity.getBrokerId().toString()))
          .body("content.[0].portfolioId", equalTo(entity.getPortfolioId().toString()))
          .body("content.[0].assetId", equalTo(entity.getAssetId()))
          .body("content.[0].operationDate", equalTo(entity.getOperationDate().toString()))
          .body("content.[0].operationType", equalTo(entity.getOperationType().toString()))
          .body("content.[0].quantity", equalTo(entity.getQuantity()))
          .body("content.[0].unitPrice",
              equalTo(entity.getUnitPrice().setScale(4, RoundingMode.HALF_UP)))
          .body("content.[0].fees", equalTo(entity.getFees()))
          .body("content.[0].netValue", equalTo(netValue.setScale(4, RoundingMode.HALF_UP)))
          .body("pageable.pageNumber", equalTo(0))
          .body("pageable.pageSize", equalTo(20))
          .body("pageable.totalElements", equalTo(1));
    }

    @Test
    void shouldReturn400ForBadRequest() {
      final String getEndpoint = CONTEXT_PATH + PATH_GET_TRANSACTIONS;
      given()
          .contentType(ContentType.JSON)
          .when()
          .post(getEndpoint)
          .then()
          .statusCode(400)
          .body("timestamp", notNullValue())
          .body("status", equalTo(HttpStatus.BAD_REQUEST.value()))
          .body("error", equalTo(HttpStatus.BAD_REQUEST.getReasonPhrase()))
          .body("path", equalTo(getEndpoint));
    }

    @Test
    void shouldReturn405ForUnsupportedMethods() {
      final String getEndpoint = CONTEXT_PATH + PATH_GET_TRANSACTIONS;
      String errorMessage = "Request method '%s' is not supported";

      given()
          .contentType(ContentType.JSON)
          .when()
          .delete(getEndpoint)
          .then().statusCode(405)
          .body("timestamp", notNullValue())
          .body("details", equalTo(errorMessage.formatted("DELETE")));

      given()
          .contentType(ContentType.JSON)
          .when()
          .put(getEndpoint)
          .then()
          .statusCode(405)
          .body("timestamp", notNullValue())
          .body("details", equalTo(errorMessage.formatted("PUT")));

      given()
          .contentType(ContentType.JSON)
          .when()
          .patch(getEndpoint)
          .then()
          .statusCode(405)
          .body("timestamp", notNullValue())
          .body("details", equalTo(errorMessage.formatted("PATCH")));
    }
  }

  @Nested
  @DisplayName("DELETE /api/v1/transactions/{transactionId}")
  class DeleteTransactionsTests {

    @Test
    void shouldDeleteTransactions() {
      TransactionEntity entity = getTransactionEntity();
      UUID id = repository.save(entity).getId();

      given()
          .contentType(ContentType.JSON)
          .when()
          .delete(CONTEXT_PATH + PATH_DELETE_TRANSACTIONS, id)
          .then()
          .statusCode(204);

    }

    @Test
    void shouldDeleteTransactionsOnce() {
      TransactionEntity entity = getTransactionEntity();
      UUID id = repository.save(entity).getId();

      given()
          .contentType(ContentType.JSON)
          .when()
          .delete(CONTEXT_PATH + PATH_DELETE_TRANSACTIONS, id)
          .then()
          .statusCode(204);

      given()
          .contentType(ContentType.JSON)
          .when()
          .delete(CONTEXT_PATH + PATH_DELETE_TRANSACTIONS, id)
          .then()
          .statusCode(404)
          .body("timestamp", notNullValue())
          .body("details", equalTo("Transaction with id %s not found".formatted(id.toString())));

      given()
          .contentType(ContentType.JSON)
          .when()
          .get(CONTEXT_PATH + PATH_GET_TRANSACTIONS)
          .then()
          .statusCode(200)
          .body("pageable.pageNumber", equalTo(0))
          .body("pageable.totalElements", equalTo(0));
    }

    @Test
    void shouldNotDeleteTransactions_whenRepositoryHasMultipleRecords_deletesOnlyOne() {
      TransactionEntity entity = getTransactionEntity();
      UUID id = repository.save(entity).getId();

      repository.save(getTransactionEntity());

      given()
          .contentType(ContentType.JSON)
          .when()
          .get(CONTEXT_PATH + PATH_GET_TRANSACTIONS)
          .then()
          .statusCode(200)
          .body("pageable.pageNumber", equalTo(0))
          .body("pageable.totalElements", equalTo(2));

      given()
          .contentType(ContentType.JSON)
          .when()
          .delete(CONTEXT_PATH + PATH_DELETE_TRANSACTIONS, id)
          .then()
          .statusCode(204);

      given()
          .contentType(ContentType.JSON)
          .when()
          .get(CONTEXT_PATH + PATH_GET_TRANSACTIONS)
          .then()
          .statusCode(200)
          .body("pageable.pageNumber", equalTo(0))
          .body("pageable.totalElements", equalTo(1));
    }

    @Test
    void shouldNotDeleteTransactions_notFound() {
      UUID id = UUID.randomUUID();

      given()
          .contentType(ContentType.JSON)
          .when()
          .delete(CONTEXT_PATH + PATH_DELETE_TRANSACTIONS, id)
          .then()
          .statusCode(404)
          .body("timestamp", notNullValue())
          .body("details", equalTo("Transaction with id %s not found".formatted(id.toString())));
    }

    @Test
    void shouldNotDeleteTransactions_badId_invalidUUID() {
      String id = "BAAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA";

      given()
          .contentType(ContentType.JSON)
          .when()
          .delete(CONTEXT_PATH + PATH_DELETE_TRANSACTIONS, id)
          .then()
          .statusCode(400)
          .body("timestamp", notNullValue())
          .body("details", equalTo("UUID string too large"));
    }

    @Test
    void shouldNotDeleteTransactions_badId_nullAsValue() {
      String id = "null";

      given()
          .contentType(ContentType.JSON)
          .when()
          .delete(CONTEXT_PATH + PATH_DELETE_TRANSACTIONS, id)
          .then()
          .statusCode(400)
          .body("timestamp", notNullValue())
          .body("details", equalTo("Invalid UUID string: null"));
    }

    @Test
    void shouldNotDeleteTransactions_unsupportedMethod() {
      final UUID id = UUID.randomUUID();
      final String deleteEndpoint = CONTEXT_PATH + PATH_DELETE_TRANSACTIONS;
      String errorMessage = "Request method '%s' is not supported";

      given()
          .contentType(ContentType.JSON)
          .when()
          .get(deleteEndpoint, id)
          .then().statusCode(405)
          .body("timestamp", notNullValue())
          .body("details", equalTo(errorMessage.formatted("GET")));

      given()
          .contentType(ContentType.JSON)
          .when()
          .put(deleteEndpoint, id)
          .then()
          .statusCode(405)
          .body("timestamp", notNullValue())
          .body("details", equalTo(errorMessage.formatted("PUT")));

      given()
          .contentType(ContentType.JSON)
          .when()
          .post(deleteEndpoint, id)
          .then()
          .statusCode(405)
          .body("timestamp", notNullValue())
          .body("details", equalTo(errorMessage.formatted("POST")));

      given()
          .contentType(ContentType.JSON)
          .when()
          .patch(deleteEndpoint, id)
          .then()
          .statusCode(405)
          .body("timestamp", notNullValue())
          .body("details", equalTo(errorMessage.formatted("PATCH")));
    }
  }

  @Nested
  @DisplayName("POST /api/v1/transactions")
  class CreateTransactionsTests {

    private static final String CREATE_ENDPOINT = CONTEXT_PATH + PATH_CREATE_TRANSACTIONS;

    @Test
    void shouldCreateTransactions() {
      TransactionRequest transactionRequest = getTransactionRequest();

      BigDecimal fees = BigDecimal.valueOf(0.03);
      BigDecimal netValue = transactionRequest.getUnitPrice()
          .multiply(BigDecimal.valueOf(transactionRequest.getQuantity()))
          .add(fees).setScale(2, RoundingMode.HALF_UP);

      postTransaction(transactionRequest)
          .then()
          .statusCode(201)
          .header("Location", containsString("/transactions/"))
          .body("id", notNullValue())
          .body("brokerId", equalTo(transactionRequest.getBrokerId().toString()))
          .body("portfolioId", equalTo(transactionRequest.getPortfolioId().toString()))
          .body("assetId", equalTo(transactionRequest.getAssetId()))
          .body("operationDate", equalTo(transactionRequest.getOperationDate().toString()))
          .body("operationType", equalTo(transactionRequest.getOperationType().toString()))
          .body("quantity", equalTo(transactionRequest.getQuantity().intValue()))
          .body("unitPrice", equalTo(transactionRequest.getUnitPrice().intValue()))
          .body("fees", equalTo(fees))
          .body("netValue", equalTo(netValue));
    }

    @Test
    void shouldReturn400_whenBodyIsEmpty() {
      postTransaction(null)
          .then()
          .statusCode(400);
    }

    @Test
    void shouldReturn405ForUnsupportedMethods() {
      String errorMessage = "Request method '%s' is not supported";

      String data = JsonMapper.builder().build().writeValueAsString(getTransactionRequest());

      given()
          .contentType(ContentType.JSON)
          .body(data)
          .when()
          .put(CREATE_ENDPOINT)
          .then()
          .statusCode(405)
          .body("timestamp", notNullValue())
          .body("details", equalTo(errorMessage.formatted("PUT")));

      given()
          .contentType(ContentType.JSON)
          .body(data)
          .when()
          .delete(CREATE_ENDPOINT)
          .then()
          .statusCode(405)
          .body("timestamp", notNullValue())
          .body("details", equalTo(errorMessage.formatted("DELETE")));

      given()
          .contentType(ContentType.JSON)
          .body(data)
          .when()
          .patch(CREATE_ENDPOINT)
          .then()
          .statusCode(405)
          .body("timestamp", notNullValue())
          .body("details", equalTo(errorMessage.formatted("PATCH")));
    }

    @Test
    void shouldReturn415_whenContentTypeIsNotJson() {
      String data = JsonMapper.builder().build().writeValueAsString(getTransactionRequest());

      given()
          .contentType(ContentType.TEXT)
          .body(data)
          .when()
          .post(CREATE_ENDPOINT)
          .then()
          .statusCode(415)
          .body("timestamp", notNullValue())
          .body("details", equalTo("Content-Type 'text/plain;charset=ISO-8859-1' is not supported"));
    }

    private Response postTransaction(TransactionRequest request) {
      return given()
          .contentType(ContentType.JSON)
          .body(JsonMapper.builder().build().writeValueAsString(request))
          .when()
          .post(CREATE_ENDPOINT);
    }

    private static @NonNull TransactionRequest getTransactionRequest() {
      TransactionRequest request = new TransactionRequest();
      request.setBrokerId(UUID.randomUUID());
      request.setPortfolioId(UUID.randomUUID());
      request.setAssetId("ASSE4");
      request.setOperationDate(LocalDate.now());
      request.setOperationType(OperationType.BUY);
      request.setQuantity(BigDecimal.TEN.intValue());
      request.setUnitPrice(BigDecimal.TEN);

      return request;
    }

  }

  private static @NonNull TransactionEntity getTransactionEntity() {
    TransactionEntity entity = new TransactionEntity();
    entity.setBrokerId(UUID.randomUUID());
    entity.setPortfolioId(UUID.randomUUID());
    entity.setAssetId("ASSE4");
    entity.setOperationDate(LocalDate.now());
    entity.setOperationType(OperationType.BUY);
    entity.setQuantity(BigDecimal.TEN.intValue());
    entity.setUnitPrice(BigDecimal.TEN);
    entity.setFees(new BigDecimal("0.03"));

    return entity;
  }

}