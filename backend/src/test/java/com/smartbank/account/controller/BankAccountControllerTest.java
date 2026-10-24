package com.smartbank.account.controller;

import com.smartbank.account.entity.BankAccount;
import com.smartbank.account.service.BankAccountService;
import com.smartbank.security.JwtService;
import com.smartbank.user.entity.User;
import com.smartbank.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BankAccountController.class)
class BankAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BankAccountService bankAccountService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtService jwtService;

    private User authenticatedUser() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        when(user.getEmail()).thenReturn("customer@example.com");
        return user;
    }

    @Test
    void shouldCreateAccountForAuthenticatedCustomer() throws Exception {
        User user = authenticatedUser();

        when(userRepository.findByEmail("customer@example.com"))
                .thenReturn(Optional.of(user));

        BankAccount account =
                new BankAccount("123456789012", user);

        when(bankAccountService.createAccount(1L))
                .thenReturn(account);

        mockMvc.perform(post("/api/accounts")
                        .with(user("customer@example.com"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountNumber")
                        .value("123456789012"))
                .andExpect(jsonPath("$.balance").value(0))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(bankAccountService).createAccount(1L);
    }

    @Test
    void shouldReturnOnlyAuthenticatedCustomersAccounts() throws Exception {
        User user = authenticatedUser();

        when(userRepository.findByEmail("customer@example.com"))
                .thenReturn(Optional.of(user));

        BankAccount account =
                new BankAccount("123456789012", user);

        when(bankAccountService.getMyAccounts(1L))
                .thenReturn(List.of(account));

        mockMvc.perform(get("/api/accounts")
                        .with(user("customer@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accountNumber")
                        .value("123456789012"))
                .andExpect(jsonPath("$[0].balance").value(0));

        verify(bankAccountService).getMyAccounts(1L);
    }

    @Test
    void shouldReturnAccountWhenItBelongsToAuthenticatedCustomer()
            throws Exception {
        User user = authenticatedUser();

        when(userRepository.findByEmail("customer@example.com"))
                .thenReturn(Optional.of(user));

        BankAccount account =
                new BankAccount("123456789012", user);

        when(bankAccountService.getMyAccount(1L, 1L))
                .thenReturn(account);

        mockMvc.perform(get("/api/accounts/1")
                        .with(user("customer@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber")
                        .value("123456789012"));

        verify(bankAccountService).getMyAccount(1L, 1L);
    }

    @Test
    void shouldRejectUnauthenticatedAccountRequest() throws Exception {
        mockMvc.perform(get("/api/accounts"))
                .andExpect(result -> {
                    int statusCode = result.getResponse().getStatus();

                    assertTrue(
                            statusCode == 401 || statusCode == 403,
                            "Expected 401 or 403, but received " + statusCode
                    );
                });
    }
}
