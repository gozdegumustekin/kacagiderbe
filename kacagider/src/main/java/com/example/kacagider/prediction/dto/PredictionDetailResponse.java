package com.example.kacagider.prediction.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

public record PredictionDetailResponse(
        UUID id,
        UUID userId,
        String il,
        String ilce,
        String mahalle,
        String emlakTipi,
        Double metrekareBrut,
        Double metrekareNet,
        JsonNode requestJson,
        JsonNode previewJson,
        JsonNode predictionJson,
        Instant createdAt) {
}