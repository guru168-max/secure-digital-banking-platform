package com.smartbank.transaction.service;

import com.smartbank.account.entity.BankAccount;
import com.smartbank.account.repository.BankAccountRepository;
import com.smartbank.transaction.dto.TransactionResponse;
import com.smartbank.transaction.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TransactionService {

    private final BankAccountRepository bankAccountRepository;
    private final TransactionRepository transactionRepository;

    public TransactionService(
            BankAccountRepository bankAccountRepository,
            TransactionRepository transactionRepository) {
        this.bankAccountRepository = bankAccountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getMyTransactions(
            Long accountId,
            Long userId) {

        BankAccount account = bankAccountRepository
                .findByIdAndUser_Id(accountId, userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Bank account not found"));

        return transactionRepository
                .findAllByBankAccount_IdOrderByCreatedAtDesc(account.getId())
                .stream()
                .map(TransactionResponse::from)
                .toList();
    }
}
