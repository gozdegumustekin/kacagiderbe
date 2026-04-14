package com.example.kacagider.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "email_verification_tokens")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerificationToken {

    @Id
    @GeneratedValue
    private UUID id;

    private UUID userId;

    private String token;

    private String code;

    private int attempts;

    private Instant expiresAt;

    private Instant usedAt;
}