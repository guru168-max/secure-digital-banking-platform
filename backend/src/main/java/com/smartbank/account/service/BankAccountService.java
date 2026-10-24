package com.smartbank.account.service;

import com.smartbank.account.entity.BankAccount;
import com.smartbank.account.repository.BankAccountRepository;
import com.smartbank.transaction.entity.Transaction;
import com.smartbank.transaction.entity.TransactionType;
import com.smartbank.transaction.repository.TransactionRepository;
import com.smartbank.user.entity.User;
import com.smartbank.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;

@Service
public class BankAccountService {

    private final BankAccountRepository bankAccountRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    private final SecureRandom secureRandom = new SecureRandom();

    public BankAccountService(
            BankAccountRepository bankAccountRepository,
            UserRepository userRepository,
            TransactionRepository transactionRepository) {
        this.bankAccountRepository = bankAccountRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public BankAccount createAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        String accountNumber = generateUniqueAccountNumber();

        BankAccount account = new BankAccount(accountNumber, user);

        return bankAccountRepository.save(account);
    }

    @Transactional(readOnly = true)
    public List<BankAccount> getMyAccounts(Long userId) {
        return bankAccountRepository.findAllByUser_Id(userId);
    }

    @Transactional(readOnly = true)
    public BankAccount getMyAccount(Long accountId, Long userId) {
        return bankAccountRepository
                .findByIdAndUser_Id(accountId, userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Account not found"));
    }

    @Transactional
    public BankAccount deposit(
            Long accountId,
            Long userId,
            BigDecimal amount) {

        BankAccount account = getMyAccount(accountId, userId);

        account.deposit(amount);

        bankAccountRepository.save(account);

        Transaction transaction = new Transaction(
                TransactionType.DEPOSIT,
                amount,
                account.getBalance(),
                account
        );

        transactionRepository.save(transaction);

        return account;
    }

    @Transactional
    public BankAccount withdraw(
            Long accountId,
            Long userId,
            BigDecimal amount) {

        BankAccount account = getMyAccount(accountId, userId);

        account.withdraw(amount);

        bankAccountRepository.save(account);

        Transaction transaction = new Transaction(
                TransactionType.WITHDRAWAL,
                amount,
                account.getBalance(),
                account
        );

        transactionRepository.save(transaction);

        return account;
    }

    private String generateUniqueAccountNumber() {
        String accountNumber;

        do {
            // Generates a 12-digit account number.
            long number = 100_000_000_000L
                    + secureRandom.nextLong(900_000_000_000L);

            accountNumber = Long.toString(number);

        } while (bankAccountRepository
                .existsByAccountNumber(accountNumber));

        return accountNumber;
    }
}
