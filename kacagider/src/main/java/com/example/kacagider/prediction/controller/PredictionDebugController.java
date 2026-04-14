package com.example.kacagider.prediction.controller;

import com.example.kacagider.prediction.dto.PredictionRequest;
import com.example.kacagider.prediction.service.PredictionInputBuilderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/prediction")
@CrossOrigin(origins = "*")
public class PredictionDebugController {

    private final PredictionInputBuilderService predictionInputBuilderService;

    public PredictionDebugController(PredictionInputBuilderService predictionInputBuilderService) {
        this.predictionInputBuilderService = predictionInputBuilderService;
    }

    @PostMapping("/preview-input")
    public ResponseEntity<Map<String, Object>> previewInput(@RequestBody PredictionRequest request) {
        return ResponseEntity.ok(predictionInputBuilderService.buildPreview(request));
    }
}