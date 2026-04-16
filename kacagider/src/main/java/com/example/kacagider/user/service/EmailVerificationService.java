package com.example.kacagider.user.service;

import com.example.kacagider.mail.MailService;
import com.example.kacagider.user.entity.EmailVerificationToken;
import com.example.kacagider.user.repo.EmailVerificationTokenRepository;
import com.example.kacagider.user.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationTokenRepository tokenRepo;
    private final UserRepository userRepo;
    private final MailService mailService;

    @Value("${app.verification.tokenMinutes:60}")
    private long tokenMinutes;

    public void createAndSendCode(UUID userId, String email) {

        // 6 haneli kod
        String code = String.format("%06d",
                new SecureRandom().nextInt(1_000_000));

        String token = UUID.randomUUID().toString().replace("-", "");

        EmailVerificationToken evt = EmailVerificationToken.builder()
                .token(token)
                .code(code)
                .userId(userId)
                .expiresAt(Instant.now().plus(tokenMinutes, ChronoUnit.MINUTES))
                .build();

        tokenRepo.save(evt);

        mailService.sendHtml(email, "Email Doğrulama Kodu", buildHtml(code));
    }

    private String buildHtml(String code) {
        return """
                <div style="font-family:Arial,sans-serif">
                    <h2>Email Doğrulama</h2>
                    <p>Doğrulama kodun:</p>
                    <h1 style="letter-spacing:4px">%s</h1>
                    <p>Bu kod %d dakika geçerlidir.</p>
                </div>
                """.formatted(code, tokenMinutes);
    }

    public void resend(String email) {
        var user = userRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı."));

        if (user.isEmailVerified()) {
            throw new IllegalArgumentException("Email zaten doğrulanmış.");
        }

        createAndSendCode(user.getId(), user.getEmail());
    }

    public void verify(String email, String code) {

        var user = userRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı."));

        EmailVerificationToken evt = tokenRepo.findTopByUserIdAndCodeAndUsedAtIsNullOrderByExpiresAtDesc(
                user.getId(), code)
                .orElseThrow(() -> new IllegalArgumentException("Kod hatalı."));

        if (evt.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Kod süresi dolmuş.");
        }

        if (evt.getAttempts() >= 5) {
            throw new IllegalArgumentException("Çok fazla hatalı deneme.");
        }

        if (!evt.getCode().equals(code)) {
            evt.setAttempts(evt.getAttempts() + 1);
            tokenRepo.save(evt);
            throw new IllegalArgumentException("Kod hatalı.");
        }

        user.setEmailVerified(true);
        userRepo.save(user);

        evt.setUsedAt(Instant.now());
        tokenRepo.save(evt);
    }
}