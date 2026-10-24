
package com.smartbank.user.controller;

import com.smartbank.security.JwtService;
import com.smartbank.user.dto.LoginRequest;
import com.smartbank.user.dto.LoginResponse;
import com.smartbank.user.dto.RegisterRequest;
import com.smartbank.user.entity.User;
import com.smartbank.user.service.AuthService;
import com.smartbank.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(
            UserService userService,
            AuthService authService,
            JwtService jwtService) {
        this.userService = userService;
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(
            @Valid @RequestBody RegisterRequest request) {

        User user = userService.registerUser(
                request.getFullName(),
                request.getEmail(),
                request.getPassword()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Registration successful. User ID: " + user.getId());
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {

        User user = authService.authenticate(
                request.getEmail(),
                request.getPassword()
        );

        String token = jwtService.generateToken(user.getEmail());

        LoginResponse response = new LoginResponse(
                "Login successful",
                token,
                "Bearer"
        );

        return ResponseEntity.ok(response);
    }
}
