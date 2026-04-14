package com.example.kacagider.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyEmailRequest {

    @NotBlank
    @Email(message = "Geçerli bir email gir.")
    private String email;

    @NotBlank
    @Pattern(regexp = "^\\d{6}$", message = "Kod 6 haneli olmalı.")
    private String code;
}