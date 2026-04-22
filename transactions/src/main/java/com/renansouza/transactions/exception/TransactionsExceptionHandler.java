package com.renansouza.transactions.exception;

import com.homewealth.transactions.model.ErrorResponse;
import java.time.OffsetDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Global exception handler for the Transactions API.
 *
 * <p>This class provides centralized exception handling across all {@code @RestController}
 * classes in the transactions module. It intercepts exceptions thrown during request processing and
 * converts them into appropriate HTTP responses with standardized error formats.</p>
 *
 * <p>The handler processes the following exception types:</p>
 * <ul>
 *   <li>{@link MethodArgumentNotValidException} - Bean validation failures (400 Bad Request)</li>
 *   <li>{@link MethodArgumentTypeMismatchException} - Type conversion errors (400 Bad Request)</li>
 *   <li>{@link TransactionNotFoundException} - Resource not found errors (404 Not Found)</li>
 *   <li>{@link HttpRequestMethodNotSupportedException} - Unsupported HTTP methods (405 Method Not Allowed)</li>
 *   <li>{@link Exception} - All other unexpected errors (500 Internal Server Error)</li>
 * </ul>
 *
 * <p>All exceptions are logged appropriately and returned to the client as JSON responses
 * conforming to the {@link ErrorResponse} structure.</p>
 *
 * <p>Example error response:</p>
 * <pre>
 * {
 *   "status": 404,
 *   "error": "Not Found",
 *   "message": "Transaction not found with ID: 123e4567-e89b-12d3-a456-426614174000",
 *   "timestamp": "2026-01-26T10:30:00"
 * }
 * </pre>
 *
 * @author Renan Alberto de Souza
 * @see RestControllerAdvice
 * @see ExceptionHandler
 * @see ErrorResponse
 * @see TransactionNotFoundException
 * @since 1.0
 */
@RestControllerAdvice
public class TransactionsExceptionHandler {

  /**
   * Handles type mismatch exceptions when request parameters cannot be converted to expected
   * types.
   *
   * <p>This exception occurs when a path variable, request parameter, or other method argument
   * cannot be converted to the declared type. For example, passing a non-numeric string where an
   * integer is expected, or an invalid UUID format.</p>
   *
   * @param ex the {@link MethodArgumentTypeMismatchException} containing conversion error details
   * @return a {@link ResponseEntity} containing an {@link ErrorResponse} with HTTP 400 status and a
   * descriptive message indicating which parameter caused the error
   */
  @ResponseStatus(value = HttpStatus.BAD_REQUEST)
  @ExceptionHandler({MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class})
  public ErrorResponse handleBadRequest(MethodArgumentTypeMismatchException ex) {
    String message = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
    return new ErrorResponse(message, OffsetDateTime.now());
  }

  /**
   * Handles exceptions thrown when a requested transaction resource cannot be found.
   *
   * <p>This handler is triggered when attempting to retrieve, update, or delete a transaction
   * that does not exist in the system. The transaction ID is typically included in the exception
   * message for debugging purposes.</p>
   *
   * @param ex the {@link TransactionNotFoundException} containing the error details
   * @return a {@link ResponseEntity} containing an {@link ErrorResponse} with HTTP 404 status and
   * the exception's detail message
   */
  @ResponseStatus(value = HttpStatus.NOT_FOUND)
  @ExceptionHandler(TransactionNotFoundException.class)
  public ErrorResponse handleResourceNotFound(TransactionNotFoundException ex) {
    return new ErrorResponse(ex.getMessage(), OffsetDateTime.now());
  }

  /**
   * Handles exceptions when an unsupported HTTP method is used on an endpoint.
   *
   * <p>This exception is thrown by Spring when a client attempts to use an HTTP method
   * (GET, POST, PUT, DELETE, etc.) that is not supported by the target endpoint. For example,
   * attempting a PUT request on an endpoint that only supports GET and POST.</p>
   *
   * @param ex the {@link HttpRequestMethodNotSupportedException} containing the unsupported method
   *           details
   * @return a {@link ResponseEntity} containing an {@link ErrorResponse} with HTTP 405 status and a
   * message indicating which method was not supported
   */
  @ResponseStatus(value = HttpStatus.METHOD_NOT_ALLOWED)
  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ErrorResponse handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
    return new ErrorResponse(ex.getMessage(), OffsetDateTime.now());
  }

  /**
   * Handles exceptions when an unsupported media type is used in a request.
   *
   * <p>This exception is thrown by Spring when a client sends a request with a
   * {@code Content-Type} that is not supported by the target endpoint. For example,
   * attempting a POST request with {@code Content-Type: text/plain} on an endpoint
   * that only accepts {@code application/json}.</p>
   *
   * @param ex the {@link HttpMediaTypeNotSupportedException} containing the unsupported
   *           media type details
   * @return a {@link ResponseEntity} containing an {@link ErrorResponse} with HTTP 415 status
   * and a message indicating which media type was not supported
   */
  @ResponseStatus(value = HttpStatus.UNSUPPORTED_MEDIA_TYPE)
  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ErrorResponse handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
    return new ErrorResponse(ex.getMessage(), OffsetDateTime.now());
  }

  /**
   * Handles all uncaught exceptions that are not handled by more specific exception handlers.
   *
   * <p>This is a catch-all handler that processes any {@link Exception} not explicitly handled
   * by other methods in this class. It prevents internal error details from being exposed to
   * clients while ensuring all exceptions are properly logged for debugging.</p>
   *
   * <p><strong>Note:</strong> The full exception stack trace is logged but not returned to the
   * client
   * to avoid exposing sensitive system information.</p>
   *
   * @param ex the {@link Exception} that was thrown
   * @return a {@link ResponseEntity} containing an {@link ErrorResponse} with HTTP 500 status and a
   * generic error message
   */
  @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
  @ExceptionHandler(Exception.class)
  public ErrorResponse handleGenericException(Exception ex) {
    return new ErrorResponse(ex.getMessage(), OffsetDateTime.now());
  }

}