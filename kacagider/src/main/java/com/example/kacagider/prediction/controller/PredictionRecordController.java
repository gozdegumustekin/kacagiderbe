package com.example.kacagider.prediction.controller;

import com.example.kacagider.prediction.dto.CreatePredictionRecordResponse;
import com.example.kacagider.prediction.dto.PredictionDetailResponse;
import com.example.kacagider.prediction.dto.PredictionHistoryItemResponse;
import com.example.kacagider.prediction.dto.PredictionRequest;
import com.example.kacagider.prediction.service.PredictionRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/predictions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PredictionRecordController {

    private final PredictionRecordService predictionRecordService;

    @PostMapping
    public ResponseEntity<?> create(
            @RequestBody PredictionRequest request,
            Authentication authentication) {
        try {
            UUID userId = currentUserId(authentication);
            CreatePredictionRecordResponse response = predictionRecordService.create(userId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(errorBody(
                    HttpStatus.BAD_REQUEST.value(),
                    "Geçersiz istek",
                    e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Kayıt oluşturulurken hata oluştu",
                    e.getMessage()));
        }
    }

    @GetMapping("/my")
    public ResponseEntity<?> myHistory(Authentication authentication) {
        try {
            UUID userId = currentUserId(authentication);
            List<PredictionHistoryItemResponse> response = predictionRecordService.getMyHistory(userId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Geçmiş kayıtlar alınırken hata oluştu",
                    e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> detail(
            @PathVariable UUID id,
            Authentication authentication) {
        try {
            UUID userId = currentUserId(authentication);
            PredictionDetailResponse response = predictionRecordService.getDetail(userId, id);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(
                    HttpStatus.NOT_FOUND.value(),
                    "Kayıt bulunamadı",
                    e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Kayıt detayı alınırken hata oluştu",
                    e.getMessage()));
        }
    }

    private UUID currentUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalArgumentException("Kullanıcı doğrulaması bulunamadı.");
        }
        return UUID.fromString(authentication.getName());
    }

    private Map<String, Object> errorBody(int status, String error, String details) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status);
        body.put("error", error);
        body.put("details", details);
        return body;
    }
}