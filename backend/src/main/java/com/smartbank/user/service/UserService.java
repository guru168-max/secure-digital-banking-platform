package com.smartbank.user.service;

import com.smartbank.user.entity.User;
import com.smartbank.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User registerUser(
            String fullName,
            String email,
            String rawPassword
    ) {
        String normalizedName = fullName.trim();
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException(
                    "An account with this email already exists."
            );
        }

        String passwordHash = passwordEncoder.encode(rawPassword);

        User user = new User(
                normalizedName,
                normalizedEmail,
                passwordHash
        );

        return userRepository.save(user);
    }
}
