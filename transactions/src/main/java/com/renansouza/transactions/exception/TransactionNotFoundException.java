package com.renansouza.transactions.exception;

/**
 * Exception thrown when a requested transaction cannot be found in the system.
 *
 * <p>This exception is typically thrown when attempting to retrieve, update, or delete
 * a transaction using an ID that does not exist in the database. It results in an HTTP
 * 404 Not Found response when handled by the application's exception handler.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * Transaction transaction = transactionRepository.findById(transactionId)
 *     .orElseThrow(() -> new TransactionNotFoundException(
 *         "Transaction not found with ID: " + transactionId));
 * </pre>
 *
 * @see RuntimeException
 * @see com.renansouza.transactions.exception.TransactionsExceptionHandler
 * @author Renan Alberto de Souza
 * @since 1.0
 */
public class TransactionNotFoundException extends RuntimeException {
  /**
   * Constructs a new TransactionNotFoundException with the specified detail message.
   *
   * @param message the detail message explaining why the transaction was not found.
   *                The message is saved for later retrieval by the {@link #getMessage()} method.
   */
  public TransactionNotFoundException(String message) {
    super(message);
  }

}