package com.example.kacagider.prediction.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PredictionResponse {
    private String predictedLabel;
    private String displayText;
    private Integer guvenlikSkoru;
    private Integer luksSkoru;
    private Integer sosyalSkoru;
    private Integer lokasyonSkoru;
    private String message;
}