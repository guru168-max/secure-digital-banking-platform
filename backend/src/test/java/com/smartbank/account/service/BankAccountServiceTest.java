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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BankAccountServiceTest {


    @Mock
    private BankAccountRepository bankAccountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private BankAccountService bankAccountService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User(
                "Test Customer",
                "customer@example.com",
                "hashed-password"
        );
    }

    @Test
    void shouldCreateAccountWithZeroBalance() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));
        when(bankAccountRepository.existsByAccountNumber(any()))
                .thenReturn(false);
        when(bankAccountRepository.save(any(BankAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BankAccount account = bankAccountService.createAccount(1L);

        assertNotNull(account);
        assertNotNull(account.getAccountNumber());
        assertEquals(12, account.getAccountNumber().length());
        assertEquals(0, account.getBalance().compareTo(BigDecimal.ZERO));
        assertEquals("ACTIVE", account.getStatus());

        verify(bankAccountRepository).save(any(BankAccount.class));
    }

    @Test
    void shouldRejectAccountCreationForUnknownUser() {
        when(userRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> bankAccountService.createAccount(999L)
        );

        verify(bankAccountRepository, never())
                .save(any(BankAccount.class));
    }

    @Test
    void shouldReturnAccountsForCustomer() {
        BankAccount account =
                new BankAccount("123456789012", user);

        when(bankAccountRepository.findAllByUser_Id(1L))
                .thenReturn(List.of(account));

        List<BankAccount> accounts =
                bankAccountService.getMyAccounts(1L);

        assertEquals(1, accounts.size());
        assertEquals("123456789012", accounts.get(0).getAccountNumber());
    }

    @Test
    void shouldRejectAccessToAnotherCustomersAccount() {
        when(bankAccountRepository.findByIdAndUser_Id(10L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> bankAccountService.getMyAccount(10L, 1L)
        );
    }

    @Test
    void shouldDepositMoneySuccessfullyAndRecordTransaction() {
        BankAccount account =
                new BankAccount("123456789012", user);

        when(bankAccountRepository.findByIdAndUser_Id(10L, 1L))
                .thenReturn(Optional.of(account));
        when(bankAccountRepository.save(any(BankAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BankAccount result = bankAccountService.deposit(
                10L, 1L, new BigDecimal("500.00")
        );

        assertEquals(
                0,
                new BigDecimal("500.00").compareTo(result.getBalance())
        );

        ArgumentCaptor<Transaction> captor =
                ArgumentCaptor.forClass(Transaction.class);

        verify(transactionRepository).save(captor.capture());

        Transaction transaction = captor.getValue();

        assertEquals(TransactionType.DEPOSIT, transaction.getType());
        assertEquals(
                0,
                new BigDecimal("500.00").compareTo(transaction.getAmount())
        );
        assertEquals(
                0,
                new BigDecimal("500.00")
                        .compareTo(transaction.getBalanceAfter())
        );
        assertSame(account, transaction.getBankAccount());

        verify(bankAccountRepository).save(account);
    }

    @Test
    void shouldRejectZeroDeposit() {
        BankAccount account =
                new BankAccount("123456789012", user);

        when(bankAccountRepository.findByIdAndUser_Id(10L, 1L))
                .thenReturn(Optional.of(account));

        assertThrows(
                IllegalArgumentException.class,
                () -> bankAccountService.deposit(10L, 1L, BigDecimal.ZERO)
        );

        verify(bankAccountRepository, never()).save(any(BankAccount.class));
        verifyNoTransactionsSaved();
    }

    @Test
    void shouldRejectNegativeDeposit() {
        BankAccount account =
                new BankAccount("123456789012", user);

        when(bankAccountRepository.findByIdAndUser_Id(10L, 1L))
                .thenReturn(Optional.of(account));

        assertThrows(
                IllegalArgumentException.class,
                () -> bankAccountService.deposit(
                        10L, 1L, new BigDecimal("-100.00")
                )
        );

        verify(bankAccountRepository, never()).save(any(BankAccount.class));
        verifyNoTransactionsSaved();
    }

    @Test
    void shouldRejectDepositIntoInactiveAccount() {
        BankAccount account =
                new BankAccount("123456789012", user);

        ReflectionTestUtils.setField(account, "status", "INACTIVE");

        when(bankAccountRepository.findByIdAndUser_Id(10L, 1L))
                .thenReturn(Optional.of(account));

        assertThrows(
                IllegalStateException.class,
                () -> bankAccountService.deposit(
                        10L, 1L, new BigDecimal("500.00")
                )
        );

        verify(bankAccountRepository, never()).save(any(BankAccount.class));
        verifyNoTransactionsSaved();
    }

    @Test
    void shouldRejectDepositIntoAnotherCustomersAccount() {
        when(bankAccountRepository.findByIdAndUser_Id(10L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> bankAccountService.deposit(
                        10L, 1L, new BigDecimal("500.00")
                )
        );

        verify(bankAccountRepository, never()).save(any(BankAccount.class));
        verifyNoTransactionsSaved();
    }

    @Test
    void shouldWithdrawMoneySuccessfullyAndRecordTransaction() {
        BankAccount account =
                new BankAccount("123456789012", user);
        account.deposit(new BigDecimal("500.00"));

        when(bankAccountRepository.findByIdAndUser_Id(10L, 1L))
                .thenReturn(Optional.of(account));
        when(bankAccountRepository.save(any(BankAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BankAccount result = bankAccountService.withdraw(
                10L, 1L, new BigDecimal("200.00")
        );

        assertEquals(
                0,
                new BigDecimal("300.00").compareTo(result.getBalance())
        );

        ArgumentCaptor<Transaction> captor =
                ArgumentCaptor.forClass(Transaction.class);

        verify(transactionRepository).save(captor.capture());

        Transaction transaction = captor.getValue();

        assertEquals(TransactionType.WITHDRAWAL, transaction.getType());
        assertEquals(
                0,
                new BigDecimal("200.00").compareTo(transaction.getAmount())
        );
        assertEquals(
                0,
                new BigDecimal("300.00")
                        .compareTo(transaction.getBalanceAfter())
        );
        assertSame(account, transaction.getBankAccount());

        verify(bankAccountRepository).save(account);
    }

    @Test
    void shouldAllowWithdrawalOfExactAvailableBalance() {
        BankAccount account =
                new BankAccount("123456789012", user);
        account.deposit(new BigDecimal("500.00"));

        when(bankAccountRepository.findByIdAndUser_Id(10L, 1L))
                .thenReturn(Optional.of(account));
        when(bankAccountRepository.save(any(BankAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BankAccount result = bankAccountService.withdraw(
                10L, 1L, new BigDecimal("500.00")
        );

        assertEquals(0, BigDecimal.ZERO.compareTo(result.getBalance()));

        verify(bankAccountRepository).save(account);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void shouldRejectZeroWithdrawal() {
        BankAccount account =
                new BankAccount("123456789012", user);
        account.deposit(new BigDecimal("500.00"));

        when(bankAccountRepository.findByIdAndUser_Id(10L, 1L))
                .thenReturn(Optional.of(account));

        assertThrows(
                IllegalArgumentException.class,
                () -> bankAccountService.withdraw(10L, 1L, BigDecimal.ZERO)
        );

        verify(bankAccountRepository, never()).save(any(BankAccount.class));
        verifyNoTransactionsSaved();
    }

    @Test
    void shouldRejectNegativeWithdrawal() {
        BankAccount account =
                new BankAccount("123456789012", user);
        account.deposit(new BigDecimal("500.00"));

        when(bankAccountRepository.findByIdAndUser_Id(10L, 1L))
                .thenReturn(Optional.of(account));

        assertThrows(
                IllegalArgumentException.class,
                () -> bankAccountService.withdraw(
                        10L, 1L, new BigDecimal("-100.00")
                )
        );

        verify(bankAccountRepository, never()).save(any(BankAccount.class));
        verifyNoTransactionsSaved();
    }

    @Test
    void shouldRejectWithdrawalWithInsufficientBalance() {
        BankAccount account =
                new BankAccount("123456789012", user);
        account.deposit(new BigDecimal("500.00"));

        when(bankAccountRepository.findByIdAndUser_Id(10L, 1L))
                .thenReturn(Optional.of(account));

        assertThrows(
                IllegalStateException.class,
                () -> bankAccountService.withdraw(
                        10L, 1L, new BigDecimal("600.00")
                )
        );

        assertEquals(
                0,
                new BigDecimal("500.00").compareTo(account.getBalance())
        );

        verify(bankAccountRepository, never()).save(any(BankAccount.class));
        verifyNoTransactionsSaved();
    }

    @Test
    void shouldRejectWithdrawalFromInactiveAccount() {
        BankAccount account =
                new BankAccount("123456789012", user);
        account.deposit(new BigDecimal("500.00"));

        ReflectionTestUtils.setField(account, "status", "INACTIVE");

        when(bankAccountRepository.findByIdAndUser_Id(10L, 1L))
                .thenReturn(Optional.of(account));

        assertThrows(
                IllegalStateException.class,
                () -> bankAccountService.withdraw(
                        10L, 1L, new BigDecimal("200.00")
                )
        );

        verify(bankAccountRepository, never()).save(any(BankAccount.class));
        verifyNoTransactionsSaved();
    }

    @Test
    void shouldRejectWithdrawalFromAnotherCustomersAccount() {
        when(bankAccountRepository.findByIdAndUser_Id(10L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> bankAccountService.withdraw(
                        10L, 1L, new BigDecimal("200.00")
                )
        );

        verify(bankAccountRepository, never()).save(any(BankAccount.class));
        verifyNoTransactionsSaved();
    }



    @Test
    void shouldRejectTransferWhenDestinationAccountDoesNotExist() {
        BankAccount sourceAccount =
                new BankAccount("111111111111", user);
        sourceAccount.deposit(new BigDecimal("500.00"));

        when(bankAccountRepository.findByIdAndUser_Id(10L, 1L))
                .thenReturn(Optional.of(sourceAccount));

        when(bankAccountRepository.findById(20L))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> bankAccountService.transfer(
                        10L, 1L, 20L, new BigDecimal("100.00")
                )
        );

        assertEquals(
                0,
                new BigDecimal("500.00").compareTo(sourceAccount.getBalance())
        );

        verify(bankAccountRepository, never())
                .save(any(BankAccount.class));

        verifyNoTransactionsSaved();
    }

    @Test
    void shouldRejectTransferToInactiveDestinationAccount() {
        BankAccount sourceAccount =
                new BankAccount("111111111111", user);
        sourceAccount.deposit(new BigDecimal("500.00"));

        User recipient = new User(
                "Recipient",
                "recipient@example.com",
                "hashed-password"
        );

        BankAccount destinationAccount =
                new BankAccount("222222222222", recipient);

        org.springframework.test.util.ReflectionTestUtils.setField(
                destinationAccount, "status", "INACTIVE"
        );

        when(bankAccountRepository.findByIdAndUser_Id(10L, 1L))
                .thenReturn(Optional.of(sourceAccount));

        when(bankAccountRepository.findById(20L))
                .thenReturn(Optional.of(destinationAccount));

        assertThrows(
                IllegalStateException.class,
                () -> bankAccountService.transfer(
                        10L, 1L, 20L, new BigDecimal("100.00")
                )
        );

        assertEquals(
                0,
                new BigDecimal("500.00").compareTo(sourceAccount.getBalance())
        );

        assertEquals(
                0,
                BigDecimal.ZERO.compareTo(destinationAccount.getBalance())
        );

        verify(bankAccountRepository, never())
                .save(any(BankAccount.class));

        verifyNoTransactionsSaved();
    }


    private void verifyNoTransactionsSaved() {
        verify(transactionRepository, never())
                .save(any(Transaction.class));
    }



    @Test
    void shouldTransferMoneySuccessfullyAndRecordBothTransactions() {
        BankAccount sourceAccount =
                new BankAccount("111111111111", user);
        sourceAccount.deposit(new BigDecimal("500.00"));

        User recipient = new User(
                "Recipient",
                "recipient@example.com",
                "hashed-password"
        );

        BankAccount destinationAccount =
                new BankAccount("222222222222", recipient);
        destinationAccount.deposit(new BigDecimal("100.00"));

        when(bankAccountRepository.findByIdAndUser_Id(10L, 1L))
                .thenReturn(Optional.of(sourceAccount));

        when(bankAccountRepository.findById(20L))
                .thenReturn(Optional.of(destinationAccount));

        when(bankAccountRepository.save(any(BankAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        bankAccountService.transfer(
                10L, 1L, 20L, new BigDecimal("200.00")
        );

        assertEquals(
                0,
                new BigDecimal("300.00").compareTo(sourceAccount.getBalance())
        );

        assertEquals(
                0,
                new BigDecimal("300.00").compareTo(destinationAccount.getBalance())
        );

        ArgumentCaptor<Transaction> captor =
                ArgumentCaptor.forClass(Transaction.class);

        verify(transactionRepository, times(2)).save(captor.capture());

        List<Transaction> transactions = captor.getAllValues();

        assertEquals(TransactionType.TRANSFER_OUT, transactions.get(0).getType());
        assertEquals(TransactionType.TRANSFER_IN, transactions.get(1).getType());

        assertEquals(
                0,
                new BigDecimal("200.00").compareTo(transactions.get(0).getAmount())
        );

        assertEquals(
                0,
                new BigDecimal("200.00").compareTo(transactions.get(1).getAmount())
        );

        assertEquals(
                0,
                new BigDecimal("300.00")
                        .compareTo(transactions.get(0).getBalanceAfter())
        );

        assertEquals(
                0,
                new BigDecimal("300.00")
                        .compareTo(transactions.get(1).getBalanceAfter())
        );

        assertSame(sourceAccount, transactions.get(0).getBankAccount());
        assertSame(destinationAccount, transactions.get(1).getBankAccount());

        verify(bankAccountRepository).save(sourceAccount);
        verify(bankAccountRepository).save(destinationAccount);
    }

    @Test
    void shouldRejectTransferToSameAccount() {
        assertThrows(
                IllegalArgumentException.class,
                () -> bankAccountService.transfer(
                        10L, 1L, 10L, new BigDecimal("100.00")
                )
        );

        verify(bankAccountRepository, never())
                .save(any(BankAccount.class));

        verifyNoTransactionsSaved();
    }

    @Test
    void shouldRejectTransferWithInsufficientBalance() {
        BankAccount sourceAccount =
                new BankAccount("111111111111", user);
        sourceAccount.deposit(new BigDecimal("100.00"));

        User recipient = new User(
                "Recipient",
                "recipient@example.com",
                "hashed-password"
        );

        BankAccount destinationAccount =
                new BankAccount("222222222222", recipient);

        when(bankAccountRepository.findByIdAndUser_Id(10L, 1L))
                .thenReturn(Optional.of(sourceAccount));

        when(bankAccountRepository.findById(20L))
                .thenReturn(Optional.of(destinationAccount));

        assertThrows(
                IllegalStateException.class,
                () -> bankAccountService.transfer(
                        10L, 1L, 20L, new BigDecimal("200.00")
                )
        );

        assertEquals(
                0,
                new BigDecimal("100.00").compareTo(sourceAccount.getBalance())
        );

        assertEquals(
                0,
                BigDecimal.ZERO.compareTo(destinationAccount.getBalance())
        );

        verify(bankAccountRepository, never())
                .save(any(BankAccount.class));

        verifyNoTransactionsSaved();
    }

}
