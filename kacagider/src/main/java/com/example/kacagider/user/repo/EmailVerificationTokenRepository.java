package com.example.kacagider.user.repo;

import com.example.kacagider.user.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationTokenRepository
                extends JpaRepository<EmailVerificationToken, UUID> {

        Optional<EmailVerificationToken> findTopByUserIdAndCodeAndUsedAtIsNullOrderByExpiresAtDesc(UUID userId,
                        String code);

}