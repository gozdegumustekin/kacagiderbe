package com.example.kacagider.prediction.dto;

import java.util.Map;

/**
 * Tahmin isteği. Frontend tüm 155 özelliği {@link #ozellikler} map'inde
 * gönderir; key = ham özellik ismi (Türkçe karakterli, ör. "Hamam"),
 * value = Boolean (true seçildi, false seçilmedi).
 *
 * <p>
 * Eski {@code List<String> seciliOzellikler} ve {@code List<String> yonler}
 * yerine artık ozellikler map'i tek başına bilgiyi taşıyor — yön de bu map'in
 * içinde gelir ("Kuzey": true gibi).
 */
public record PredictionRequest(
                String il,
                String ilce,
                String mahalle,
                String emlakTipi,
                Double metrekareBrut,
                Double metrekareNet,
                String odaSayisiRaw,
                String binaYasiRaw,
                String bulunduguKatRaw,
                Integer katSayisi,
                String isitmaRaw,
                Integer banyoSayisi,
                String mutfakRaw,

                Boolean balkon,
                Boolean asansor,
                Boolean otopark,
                Boolean esyali,

                Map<String, Boolean> ozellikler) {

        /** Null safety için boş map fallback'i sağlayan helper. */
        public Map<String, Boolean> ozelliklerOrEmpty() {
                return ozellikler == null ? Map.of() : ozellikler;
        }
}