package com.example.kacagider.prediction.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

public record PredictionHistoryItemResponse(
        UUID id,
        String il,
        String ilce,
        String mahalle,
        String emlakTipi,
        Double metrekareBrut,
        Double metrekareNet,
        JsonNode predictionJson,
        Instant createdAt) {
}