package com.example.kacagider.prediction.controller;

import com.example.kacagider.prediction.entity.PredictionImage;
import com.example.kacagider.prediction.service.MlImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * FastAPI ML servisinin doğrulanmış fotoğrafları gönderdiği endpoint.
 *
 * <p>
 * Akış: Frontend → FastAPI (4 aşama doğrulama) → geçerliyse FastAPI bu
 * endpoint'e POST eder. Kullanıcının JWT'si pass-through edilir, bu yüzden
 * normal Authentication ile çalışır (mevcut SecurityConfig korunur).
 */
@RestController
@RequestMapping("/api/predictions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MlImageController {

    private final MlImageService mlImageService;

    @PostMapping(value = "/{id}/images-from-ml", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> receiveFromMl(
            @PathVariable UUID id,
            @RequestPart("image") MultipartFile image,
            @RequestParam("odaTipi") String odaTipi,
            @RequestParam("kaliteEtiket") String kaliteEtiket,
            @RequestParam(value = "kaliteSkor", required = false) Double kaliteSkor,
            @RequestParam(value = "kaliteGuven", required = false) Double kaliteGuven,
            Authentication authentication) {
        try {
            UUID userId = currentUserId(authentication);
            PredictionImage saved = mlImageService.kaydetMlFotograf(
                    userId, id, image, odaTipi, kaliteEtiket, kaliteSkor, kaliteGuven);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("imageId", saved.getId());
            body.put("odaTipi", saved.getOdaTipi());
            body.put("kaliteEtiket", saved.getResnetLabel());
            body.put("publicUrl", saved.getPublicUrl());
            body.put("status", saved.getResnetStatus());
            return ResponseEntity.status(HttpStatus.CREATED).body(body);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(err(HttpStatus.BAD_REQUEST.value(),
                    "Geçersiz istek", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    err(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "ML fotoğrafı kaydedilirken hata", e.getMessage()));
        }
    }

    /** Bir tahmin için oda tipi bazında kalite ortalamalarını döndürür. */
    @GetMapping("/{id}/oda-kaliteleri")
    public ResponseEntity<?> odaKaliteleri(
            @PathVariable UUID id,
            Authentication authentication) {
        try {
            // userId yetki kontrolü için MlImageService içinde kayıt sahipliği
            // ayrıca doğrulanabilir; burada sade tutuyoruz.
            Map<String, String> kaliteler = mlImageService.odaKaliteOrtalamalari(id);
            return ResponseEntity.ok(kaliteler);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    err(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "Oda kaliteleri alınamadı", e.getMessage()));
        }
    }

    private UUID currentUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalArgumentException("Kullanıcı doğrulaması bulunamadı.");
        }
        return UUID.fromString(authentication.getName());
    }

    private Map<String, Object> err(int status, String error, String details) {
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("status", status);
        b.put("error", error);
        b.put("details", details);
        return b;
    }
}
