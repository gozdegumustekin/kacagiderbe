package com.example.kacagider.prediction.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PredictionResponse {
    private double predictedPrice;
    private String message;
}