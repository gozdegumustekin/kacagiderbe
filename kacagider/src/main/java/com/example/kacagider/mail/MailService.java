package com.example.kacagider.mail;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    public void sendHtml(String to, String subject, String html) {
        try {
            System.out.println("MAIL GONDERIMI BASLADI -> to=" + to + ", subject=" + subject);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);

            mailSender.send(message);

            System.out.println("MAIL GONDERILDI -> " + to);
        } catch (Exception e) {
            System.out.println("MAIL HATASI -> " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Mail gönderilemedi", e);
        }
    }
}