package com.smartbank.account.service;

import com.smartbank.account.entity.BankAccount;
import com.smartbank.account.repository.BankAccountRepository;
import com.smartbank.transaction.entity.Transaction;
import com.smartbank.transaction.entity.TransactionType;
import com.smartbank.transaction.repository.TransactionRepository;
import com.smartbank.user.entity.User;
import com.smartbank.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;

@SpringBootTest
@ActiveProfiles("test")
class BankAccountTransferIntegrationTest {

    @Autowired
    private BankAccountService bankAccountService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @MockitoSpyBean
    private TransactionRepository transactionRepository;

    private Long userId;
    private Long sourceAccountId;
    private Long destinationAccountId;

    @BeforeEach
    void setUp() {
        reset(transactionRepository);

        transactionRepository.deleteAll();
        bankAccountRepository.deleteAll();
        userRepository.deleteAll();

        User user = userRepository.save(
                new User(
                        "Transfer Test User",
                        "transfer-test@example.com",
                        "test-password-hash"
                )
        );

        BankAccount source = new BankAccount(
                "900000000001", user
        );
        source.deposit(new BigDecimal("1000.00"));
        source = bankAccountRepository.save(source);

        BankAccount destination = bankAccountRepository.save(
                new BankAccount("900000000002", user)
        );

        userId = user.getId();
        sourceAccountId = source.getId();
        destinationAccountId = destination.getId();
    }

    @Test
    void successfulTransferUpdatesBothBalancesAndRecordsTransactions() {
        bankAccountService.transfer(
                sourceAccountId,
                userId,
                destinationAccountId,
                new BigDecimal("200.00")
        );

        BankAccount source = bankAccountRepository
                .findById(sourceAccountId)
                .orElseThrow();

        BankAccount destination = bankAccountRepository
                .findById(destinationAccountId)
                .orElseThrow();

        assertEquals(
                0,
                new BigDecimal("800.00").compareTo(source.getBalance())
        );

        assertEquals(
                0,
                new BigDecimal("200.00").compareTo(destination.getBalance())
        );

        assertEquals(2, transactionRepository.count());

        assertEquals(
                1,
                transactionRepository
                        .findAllByBankAccount_IdOrderByCreatedAtDesc(
                                sourceAccountId
                        ).size()
        );

        assertEquals(
                1,
                transactionRepository
                        .findAllByBankAccount_IdOrderByCreatedAtDesc(
                                destinationAccountId
                        ).size()
        );
    }

    @Test
    void insufficientFundsLeavesBalancesAndTransactionsUnchanged() {
        assertThrows(
                IllegalStateException.class,
                () -> bankAccountService.transfer(
                        sourceAccountId,
                        userId,
                        destinationAccountId,
                        new BigDecimal("1500.00")
                )
        );

        BankAccount source = bankAccountRepository
                .findById(sourceAccountId)
                .orElseThrow();

        BankAccount destination = bankAccountRepository
                .findById(destinationAccountId)
                .orElseThrow();

        assertEquals(
                0,
                new BigDecimal("1000.00").compareTo(source.getBalance())
        );

        assertEquals(
                0,
                BigDecimal.ZERO.compareTo(destination.getBalance())
        );

        assertEquals(0, transactionRepository.count());
    }

    @Test
    void databaseFailureRollsBackTransfer() {
        doAnswer(invocation -> {
            Transaction transaction = invocation.getArgument(0);

            if (transaction.getType() == TransactionType.TRANSFER_IN) {
                throw new RuntimeException(
                        "Simulated database failure"
                );
            }

            return invocation.callRealMethod();
        }).when(transactionRepository).save(any(Transaction.class));

        assertThrows(
                RuntimeException.class,
                () -> bankAccountService.transfer(
                        sourceAccountId,
                        userId,
                        destinationAccountId,
                        new BigDecimal("200.00")
                )
        );

        BankAccount source = bankAccountRepository
                .findById(sourceAccountId)
                .orElseThrow();

        BankAccount destination = bankAccountRepository
                .findById(destinationAccountId)
                .orElseThrow();

        assertEquals(
                0,
                new BigDecimal("1000.00").compareTo(source.getBalance())
        );

        assertEquals(
                0,
                BigDecimal.ZERO.compareTo(destination.getBalance())
        );

        assertEquals(0, transactionRepository.count());
    }
}
