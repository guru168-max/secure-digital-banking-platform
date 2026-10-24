package com.smartbank.transaction.repository;

import com.smartbank.transaction.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository
        extends JpaRepository<Transaction, Long> {

    List<Transaction> findAllByBankAccount_IdOrderByCreatedAtDesc(
            Long accountId
    );
}
