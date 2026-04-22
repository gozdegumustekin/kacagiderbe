package com.example.kacagider.prediction.dto;

import java.time.Instant;
import java.util.UUID;

public record PredictionImageResponse(
        UUID id,
        UUID predictionRecordId,
        String originalFilename,
        String contentType,
        Long sizeBytes,
        Integer sortOrder,
        String storagePath,
        String publicUrl,
        String resnetStatus,
        String resnetLabel,
        Double resnetScore,
        Instant createdAt) {
}