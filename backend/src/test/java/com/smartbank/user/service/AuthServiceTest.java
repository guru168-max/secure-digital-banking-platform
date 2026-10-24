package com.smartbank.user.service;

import com.smartbank.user.entity.User;
import com.smartbank.user.exception.InvalidCredentialsException;
import com.smartbank.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthService(userRepository, passwordEncoder);

        user = new User(
                "Guru",
                "guru@gmail.com",
                passwordEncoder.encode("GuruBank@123")
        );
    }

    @Test
    void shouldAuthenticateWithCorrectCredentials() {
        when(userRepository.findByEmail("guru@gmail.com"))
                .thenReturn(Optional.of(user));

        User result = authService.authenticate(
                "guru@gmail.com",
                "GuruBank@123"
        );

        assertNotNull(result);
        assertEquals("guru@gmail.com", result.getEmail());

        verify(userRepository).findByEmail("guru@gmail.com");
    }

    @Test
    void shouldRejectIncorrectPassword() {
        when(userRepository.findByEmail("guru@gmail.com"))
                .thenReturn(Optional.of(user));

        assertThrows(
                InvalidCredentialsException.class,
                () -> authService.authenticate(
                        "guru@gmail.com",
                        "WrongPassword@123"
                )
        );
    }

    @Test
    void shouldRejectUnknownEmail() {
        when(userRepository.findByEmail("unknown@gmail.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                InvalidCredentialsException.class,
                () -> authService.authenticate(
                        "unknown@gmail.com",
                        "GuruBank@123"
                )
        );
    }

    @Test
    void shouldNormalizeEmailBeforeLookup() {
        when(userRepository.findByEmail("guru@gmail.com"))
                .thenReturn(Optional.of(user));

        User result = authService.authenticate(
                "  GURU@GMAIL.COM  ",
                "GuruBank@123"
        );

        assertNotNull(result);
        verify(userRepository).findByEmail("guru@gmail.com");
    }
}
