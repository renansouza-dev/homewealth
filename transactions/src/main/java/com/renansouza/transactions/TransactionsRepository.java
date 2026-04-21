package com.renansouza.transactions;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing {@link TransactionEntity} persistence operations.
 *
 * <p>This repository extends {@link JpaRepository} for standard CRUD operations and
 * {@link JpaSpecificationExecutor} to support dynamic queries using the Specification pattern,
 * enabling flexible filtering and searching capabilities.
 *
 * @author Renan Alberto de Souza
 * @since 1.0
 */
@Repository
public interface TransactionsRepository extends JpaRepository<TransactionEntity, UUID>,
    JpaSpecificationExecutor<TransactionEntity> {

}