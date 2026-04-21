package com.example.kacagider.prediction.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record CreatePredictionRecordResponse(
        UUID id,
        Instant createdAt,
        PredictionResponse prediction,
        Map<String, Object> preview) {
}