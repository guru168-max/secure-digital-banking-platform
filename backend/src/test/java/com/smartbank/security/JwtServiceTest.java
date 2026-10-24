package com.smartbank.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        byte[] secretBytes = new byte[32];

        for (int i = 0; i < secretBytes.length; i++) {
            secretBytes[i] = (byte) (i + 1);
        }

        String testSecret = Base64.getEncoder()
                .encodeToString(secretBytes);

        jwtService = new JwtService(testSecret, 60_000);
    }

    @Test
    void shouldGenerateTokenContainingEmail() {
        String token = jwtService.generateToken("guru@gmail.com");

        assertNotNull(token);
        assertEquals("guru@gmail.com", jwtService.extractEmail(token));
    }

    @Test
    void shouldValidateTokenForCorrectEmail() {
        String token = jwtService.generateToken("guru@gmail.com");

        assertTrue(jwtService.isTokenValid(token, "guru@gmail.com"));
    }

    @Test
    void shouldRejectTokenForDifferentEmail() {
        String token = jwtService.generateToken("guru@gmail.com");

        assertFalse(jwtService.isTokenValid(token, "other@gmail.com"));
    }

    @Test
    void shouldRejectExpiredToken() throws InterruptedException {
        byte[] secretBytes = new byte[32];

        for (int i = 0; i < secretBytes.length; i++) {
            secretBytes[i] = (byte) (i + 1);
        }

        String testSecret = Base64.getEncoder()
                .encodeToString(secretBytes);

        JwtService shortLivedService = new JwtService(testSecret, 1);

        String token = shortLivedService.generateToken("guru@gmail.com");

        TimeUnit.MILLISECONDS.sleep(20);

        assertThrows(
                io.jsonwebtoken.ExpiredJwtException.class,
                () -> shortLivedService.extractEmail(token)
        );
    }
}
