package com.smartbank.account.service;

import com.smartbank.account.entity.BankAccount;
import com.smartbank.account.repository.BankAccountRepository;
import com.smartbank.user.entity.User;
import com.smartbank.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
        assertEquals(
                "123456789012",
                accounts.get(0).getAccountNumber()
        );
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
    void shouldDepositMoneySuccessfully() {
        BankAccount account =
                new BankAccount("123456789012", user);

        when(bankAccountRepository.findByIdAndUser_Id(10L, 1L))
                .thenReturn(Optional.of(account));

        when(bankAccountRepository.save(any(BankAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BankAccount result = bankAccountService.deposit(
                10L,
                1L,
                new BigDecimal("500.00")
        );

        assertEquals(
                0,
                new BigDecimal("500.00").compareTo(result.getBalance())
        );

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
                () -> bankAccountService.deposit(
                        10L,
                        1L,
                        BigDecimal.ZERO
                )
        );

        verify(bankAccountRepository, never())
                .save(any(BankAccount.class));
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
                        10L,
                        1L,
                        new BigDecimal("-100.00")
                )
        );

        verify(bankAccountRepository, never())
                .save(any(BankAccount.class));
    }

    @Test
    void shouldRejectDepositIntoInactiveAccount() {
        BankAccount account =
                new BankAccount("123456789012", user);

        // Mark the account inactive using reflection for this unit test.
        org.springframework.test.util.ReflectionTestUtils.setField(
                account,
                "status",
                "INACTIVE"
        );

        when(bankAccountRepository.findByIdAndUser_Id(10L, 1L))
                .thenReturn(Optional.of(account));

        assertThrows(
                IllegalStateException.class,
                () -> bankAccountService.deposit(
                        10L,
                        1L,
                        new BigDecimal("500.00")
                )
        );

        verify(bankAccountRepository, never())
                .save(any(BankAccount.class));
    }

    @Test
    void shouldRejectDepositIntoAnotherCustomersAccount() {
        when(bankAccountRepository.findByIdAndUser_Id(10L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> bankAccountService.deposit(
                        10L,
                        1L,
                        new BigDecimal("500.00")
                )
        );

        verify(bankAccountRepository, never())
                .save(any(BankAccount.class));
    }
}
