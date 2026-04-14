package com.example.kacagider.prediction.controller;

import com.example.kacagider.prediction.dto.PredictionRequest;
import com.example.kacagider.prediction.dto.PredictionResponse;
import com.example.kacagider.prediction.service.PredictionService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/prediction")
@CrossOrigin(origins = "*") // Gerekirse kendi CorsConfig'inize göre ayarlayın
public class PredictionController {

    private final PredictionService predictionService;

    @Autowired
    public PredictionController(PredictionService predictionService) {
        this.predictionService = predictionService;
    }

    @PostMapping("/estimate")
    public ResponseEntity<PredictionResponse> estimatePrice(@RequestBody PredictionRequest request) {
        try {
            double predictedPrice = predictionService.predictPrice(request);

            // Tahmin edilen fiyatı düzgün formata çevirebilirsiniz
            long roundedPrice = Math.round(predictedPrice);

            return ResponseEntity.ok(new PredictionResponse(roundedPrice, "Tahmin başarıyla oluşturuldu."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new PredictionResponse(0, "Hata: " + e.getMessage()));
        }
    }
}