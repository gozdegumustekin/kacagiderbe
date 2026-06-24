package com.example.kacagider.prediction.controller;

import com.example.kacagider.prediction.dto.PredictionRequest;
import com.example.kacagider.prediction.dto.PredictionResponse;
import com.example.kacagider.prediction.metadata.ModelStrategyConfig;
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
    private final PredictionInputBuilderService inputBuilder;
    private final ModelStrategyConfig strategyConfig;

    public PredictionController(PredictionService predictionService,
            PredictionInputBuilderService inputBuilder,
            ModelStrategyConfig strategyConfig) {
        this.predictionService = predictionService;
        this.inputBuilder = inputBuilder;
        this.strategyConfig = strategyConfig;
    }

    @PostMapping("/predict")
    public ResponseEntity<?> predict(@RequestBody PredictionRequest request,
            @RequestParam(name = "strateji", required = false) String strateji) {
        try {
            validateRequest(request);
            // ⬇️ DEĞİŞEN SATIR: predict(request, strateji) yerine predict(request, null,
            // strateji)
            PredictionResponse response = predictionService.predict(request, null, strateji);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(errorBody(
                    HttpStatus.BAD_REQUEST.value(), "Geçersiz istek", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorBody(
                    HttpStatus.SERVICE_UNAVAILABLE.value(),
                    "Tahmin modeli hazır değil", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Tahmin sırasında hata", e.getMessage()));
        }
    }

    @PostMapping("/preview-input")
    public ResponseEntity<?> previewInput(@RequestBody PredictionRequest request) {
        try {
            validateRequest(request);
            return ResponseEntity.ok(inputBuilder.buildPreview(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(errorBody(
                    HttpStatus.BAD_REQUEST.value(), "Geçersiz istek", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Önizleme hatası", e.getMessage()));
        }
    }

    @GetMapping("/strateji")
    public ResponseEntity<Map<String, Object>> stratejiBilgisi() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("aktif", strategyConfig.getAktifStrateji());
        body.put("mevcut", strategyConfig.tumStratejiler());

        Map<String, Object> hazirlik = new LinkedHashMap<>();
        for (String s : strategyConfig.tumStratejiler()) {
            hazirlik.put(s, predictionService.stratejiHazirMi(s));
        }
        body.put("hazir", hazirlik);
        return ResponseEntity.ok(body);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "UP");
        response.put("service", "prediction");
        response.put("modelReady", predictionService.isModelHazir());
        response.put("aktifStrateji", predictionService.getAktifStrateji());
        response.put("message", predictionService.isModelHazir()
                ? "Prediction service çalışıyor — model(ler) yüklü."
                : "Prediction service çalışıyor — model henüz yüklenmedi.");
        return ResponseEntity.ok(response);
    }

    private void validateRequest(PredictionRequest request) {
        if (request == null)
            throw new IllegalArgumentException("Request body boş olamaz.");
        if (isBlank(request.il()))
            throw new IllegalArgumentException("İl alanı zorunludur.");
        if (isBlank(request.ilce()))
            throw new IllegalArgumentException("İlçe alanı zorunludur.");
        if (isBlank(request.emlakTipi()))
            throw new IllegalArgumentException("Emlak tipi zorunludur.");
        if (request.metrekareBrut() == null || request.metrekareBrut() <= 0)
            throw new IllegalArgumentException("Brüt metrekare 0'dan büyük olmalıdır.");
        if (request.metrekareNet() == null || request.metrekareNet() <= 0)
            throw new IllegalArgumentException("Net metrekare 0'dan büyük olmalıdır.");
        if (request.katSayisi() == null || request.katSayisi() < 0)
            throw new IllegalArgumentException("Kat sayısı negatif olamaz.");
        if (request.banyoSayisi() == null || request.banyoSayisi() < 0)
            throw new IllegalArgumentException("Banyo sayısı negatif olamaz.");
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