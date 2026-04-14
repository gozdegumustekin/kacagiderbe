package com.example.kacagider.auth.controller;

import com.example.kacagider.auth.dto.AuthResponse;
import com.example.kacagider.auth.dto.LoginRequest;
import com.example.kacagider.auth.dto.RegisterRequest;
import com.example.kacagider.auth.dto.VerifyEmailRequest;
import com.example.kacagider.auth.service.AuthService;
import com.example.kacagider.user.service.EmailVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest req) {
        authService.register(req);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest req) {
        emailVerificationService.verify(req.getEmail(), req.getCode());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/resend-code")
    public ResponseEntity<Void> resendCode(@RequestParam String email) {
        emailVerificationService.resend(email);
        return ResponseEntity.ok().build();
    }
}