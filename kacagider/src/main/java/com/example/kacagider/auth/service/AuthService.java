package com.example.kacagider.auth.service;

import com.example.kacagider.auth.dto.AuthResponse;
import com.example.kacagider.auth.dto.LoginRequest;
import com.example.kacagider.auth.dto.RegisterRequest;
import com.example.kacagider.security.JwtService;
import com.example.kacagider.user.entity.User;
import com.example.kacagider.user.repo.UserRepository;
import com.example.kacagider.user.service.EmailVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailVerificationService emailVerificationService;

    public void register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();

        if (userRepo.existsByEmail(email)) {
            throw new IllegalArgumentException("Bu email zaten kayıtlı.");
        }

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(req.password()))
                .emailVerified(false)
                .createdAt(Instant.now())
                .build();

        userRepo.save(user);

        emailVerificationService.createAndSendCode(user.getId(), user.getEmail());
    }

    public AuthResponse login(LoginRequest req) {
        String email = req.email().trim().toLowerCase();

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Email veya şifre hatalı."));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Email veya şifre hatalı.");
        }

        if (!user.isEmailVerified()) {
            throw new IllegalArgumentException("Email doğrulanmamış. Lütfen mail kutunu kontrol et.");
        }

        String token = jwtService.generateAccessToken(user.getId().toString(), user.getEmail());
        return new AuthResponse(token);
    }
}