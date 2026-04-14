package com.example.kacagider.prediction.dto;

import lombok.Data;
import java.util.Map;

@Data
public class PredictionRequest {

    /**
     * Flutter'dan gelen JSON içindeki tüm anahtar-değer (Key-Value) çiftlerini
     * dinamik olarak tutan sözlük yapısı.
     * * Örnek:
     * "il" -> "Canakkale"
     * "oda_sayisi_raw" -> "3+1"
     * "Yuzme_Havuzu_Acik" -> "var"
     */
    private Map<String, Object> features;

}