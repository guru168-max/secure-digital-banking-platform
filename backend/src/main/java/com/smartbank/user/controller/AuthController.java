package com.smartbank.user.controller;

import com.smartbank.user.dto.RegisterRequest;
import com.smartbank.user.entity.User;
import com.smartbank.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        User user = userService.registerUser(
                request.getFullName(),
                request.getEmail(),
                request.getPassword()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body("Registration successful. User ID: " + user.getId());
    }
}
