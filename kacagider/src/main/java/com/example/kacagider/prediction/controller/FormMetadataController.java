package com.example.kacagider.prediction.controller;

import com.example.kacagider.prediction.dto.FormMetadataResponse;
import com.example.kacagider.prediction.metadata.PredictionFeatureCatalog;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/prediction")
@CrossOrigin(origins = "*")
public class FormMetadataController {

    @GetMapping("/form-data")
    public ResponseEntity<FormMetadataResponse> getFormData() {
        FormMetadataResponse response = new FormMetadataResponse(
                PredictionFeatureCatalog.TEMEL_ALANLAR,
                PredictionFeatureCatalog.TEMEL_BINARY_ALANLAR,
                PredictionFeatureCatalog.YONLER,
                PredictionFeatureCatalog.SKOR_GRUPLARI,
                PredictionFeatureCatalog.DIGER_OZELLIK_GRUPLARI);

        return ResponseEntity.ok(response);
    }
}