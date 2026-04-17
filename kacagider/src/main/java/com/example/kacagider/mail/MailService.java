package com.example.kacagider.mail;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MailService {

    private static final String BREVO_URL = "https://api.brevo.com/v3/smtp/email";

    private final ObjectMapper objectMapper;

    @Value("${BREVO_API_KEY}")
    private String brevoApiKey;

    @Value("${APP_MAIL_FROM}")
    private String fromEmail;

    @Value("${APP_MAIL_FROM_NAME:Kaça Gider}")
    private String fromName;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    public void sendHtml(String to, String subject, String html) {
        try {
            System.out.println("MAIL GONDERIMI BASLADI -> to=" + to + ", subject=" + subject);

            Map<String, Object> payload = Map.of(
                    "sender", Map.of(
                            "name", fromName,
                            "email", fromEmail),
                    "to", List.of(
                            Map.of("email", to)),
                    "subject", subject,
                    "htmlContent", html);

            String jsonBody = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BREVO_URL))
                    .timeout(Duration.ofSeconds(20))
                    .header("accept", "application/json")
                    .header("content-type", "application/json")
                    .header("api-key", brevoApiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString());

            int status = response.statusCode();
            String body = response.body();

            if (status >= 200 && status < 300) {
                System.out.println("MAIL GONDERILDI -> " + to + " | status=" + status + " | body=" + body);
                return;
            }

            System.out.println("MAIL HATASI -> status=" + status + " | body=" + body);
            throw new RuntimeException("Brevo mail gonderimi basarisiz. Status=" + status + ", body=" + body);

        } catch (Exception e) {
            System.out.println("MAIL HATASI -> " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Mail gönderilemedi: " + e.getMessage(), e);
        }
    }
}