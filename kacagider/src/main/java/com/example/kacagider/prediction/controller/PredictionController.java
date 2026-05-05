package com.example.kacagider.prediction.controller;

import com.example.kacagider.prediction.dto.PredictionRequest;
import com.example.kacagider.prediction.dto.PredictionResponse;
import com.example.kacagider.prediction.service.PredictionInputBuilderService;
import com.example.kacagider.prediction.service.PredictionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/prediction")
@CrossOrigin(origins = "*")
public class PredictionController {

    private final PredictionService predictionService;
    private final PredictionInputBuilderService predictionInputBuilderService;

    public PredictionController(PredictionService predictionService,
            PredictionInputBuilderService predictionInputBuilderService) {
        this.predictionService = predictionService;
        this.predictionInputBuilderService = predictionInputBuilderService;
    }

    @PostMapping("/predict")
    public ResponseEntity<?> predict(@RequestBody PredictionRequest request) {
        try {
            validateRequest(request);
            PredictionResponse response = predictionService.predict(request);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(errorBody(
                    HttpStatus.BAD_REQUEST.value(),
                    "Geçersiz istek",
                    e.getMessage()));
        } catch (IllegalStateException e) {
            // Model henüz yüklenmediyse 503 döndür — frontend bu durumu UI'da
            // "model hazır değil" şeklinde gösterebilir.
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorBody(
                    HttpStatus.SERVICE_UNAVAILABLE.value(),
                    "Tahmin modeli henüz hazır değil",
                    e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Tahmin işlemi sırasında hata oluştu",
                    e.getMessage()));
        }
    }

    @PostMapping("/preview-input")
    public ResponseEntity<?> previewInput(@RequestBody PredictionRequest request) {
        try {
            validateRequest(request);
            Map<String, Object> preview = predictionInputBuilderService.buildPreview(request);
            return ResponseEntity.ok(preview);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(errorBody(
                    HttpStatus.BAD_REQUEST.value(),
                    "Geçersiz istek",
                    e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Önizleme oluşturulurken hata oluştu",
                    e.getMessage()));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "UP");
        response.put("service", "prediction");
        response.put("modelReady", predictionService.isModelHazir());
        response.put("message", predictionService.isModelHazir()
                ? "Prediction service çalışıyor — model yüklü."
                : "Prediction service çalışıyor — model henüz yüklenmedi (predict devre dışı).");
        return ResponseEntity.ok(response);
    }

    private void validateRequest(PredictionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body boş olamaz.");
        }
        if (isBlank(request.il())) {
            throw new IllegalArgumentException("İl alanı zorunludur.");
        }
        if (isBlank(request.ilce())) {
            throw new IllegalArgumentException("İlçe alanı zorunludur.");
        }
        if (isBlank(request.emlakTipi())) {
            throw new IllegalArgumentException("Emlak tipi alanı zorunludur.");
        }
        if (request.metrekareBrut() == null || request.metrekareBrut() <= 0) {
            throw new IllegalArgumentException("Brüt metrekare 0'dan büyük olmalıdır.");
        }
        if (request.metrekareNet() == null || request.metrekareNet() <= 0) {
            throw new IllegalArgumentException("Net metrekare 0'dan büyük olmalıdır.");
        }
        if (request.katSayisi() == null || request.katSayisi() < 0) {
            throw new IllegalArgumentException("Kat sayısı negatif olamaz.");
        }
        if (request.banyoSayisi() == null || request.banyoSayisi() < 0) {
            throw new IllegalArgumentException("Banyo sayısı negatif olamaz.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private Map<String, Object> errorBody(int status, String error, String details) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status);
        body.put("error", error);
        body.put("details", details);
        return body;
    }
}