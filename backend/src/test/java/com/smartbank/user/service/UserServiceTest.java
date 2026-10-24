
package com.smartbank.user.service;

import com.smartbank.user.entity.User;
import com.smartbank.user.exception.DuplicateEmailException;
import com.smartbank.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        userService = new UserService(userRepository, passwordEncoder);
    }

    @Test
    void shouldRegisterUserAndHashPassword() {
        when(userRepository.existsByEmail("customer@example.com"))
                .thenReturn(false);

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User savedUser = userService.registerUser(
                "Test Customer",
                " CUSTOMER@EXAMPLE.COM ",
                "StrongPassword123!"
        );

        assertEquals("Test Customer", savedUser.getFullName());
        assertEquals("customer@example.com", savedUser.getEmail());

        // The stored password must not be the raw password.
        assertNotEquals(
                "StrongPassword123!",
                savedUser.getPasswordHash()
        );

        // Verify the hash can validate the original password.
        assertTrue(passwordEncoder.matches(
                "StrongPassword123!",
                savedUser.getPasswordHash()
        ));

        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldRejectDuplicateEmail() {
        when(userRepository.existsByEmail("customer@example.com"))
                .thenReturn(true);

        DuplicateEmailException exception = assertThrows(
                DuplicateEmailException.class,
                () -> userService.registerUser(
                        "Test Customer",
                        "customer@example.com",
                        "StrongPassword123!"
                )
        );

        assertEquals(
                "An account with this email already exists.",
                exception.getMessage()
        );

        // A duplicate registration must never save another user.
        verify(userRepository, never()).save(any(User.class));
    }
}
