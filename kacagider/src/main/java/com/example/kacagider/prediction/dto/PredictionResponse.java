package com.example.kacagider.prediction.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class PredictionResponse {
    private String predictedLabel;
    private String displayText;

    /**
     * Pipeline'daki her skor grubu için seçilen özellik sayısı.
     * Pipeline grupları değiştiğinde response yapısı otomatik adapte olur.
     * Örn: {"guvenlik": 3, "luks": 2, "sosyal": 0, "ulasim": 5, ...}
     */
    private Map<String, Integer> skorCounts;

    private String message;
}