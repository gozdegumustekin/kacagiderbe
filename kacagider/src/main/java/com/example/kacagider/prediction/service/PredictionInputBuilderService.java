package com.example.kacagider.prediction.service;

import com.example.kacagider.prediction.dto.PredictionRequest;
import com.example.kacagider.prediction.metadata.PredictionFeatureCatalog;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PredictionInputBuilderService {

    public Map<String, Object> buildModelInput(PredictionRequest request) {
        Set<String> selectedDirections = normalizeToSet(request.yonler());
        Set<String> selectedFeatures = normalizeToSet(request.seciliOzellikler());

        LinkedHashMap<String, Object> input = new LinkedHashMap<>();

        input.put("il", safeString(request.il()));
        input.put("ilce", safeString(request.ilce()));
        input.put("mahalle", safeString(request.mahalle()));
        input.put("emlak_tipi", safeString(request.emlakTipi()));

        input.put("metrekare_brut", request.metrekareBrut());
        input.put("metrekare_net", request.metrekareNet());
        input.put("oda_sayisi_raw", safeString(request.odaSayisiRaw()));
        input.put("bina_yasi_raw", safeString(request.binaYasiRaw()));
        input.put("bulundugu_kat_raw", safeString(request.bulunduguKatRaw()));
        input.put("kat_sayisi", request.katSayisi());
        input.put("isitma_raw", safeString(request.isitmaRaw()));
        input.put("banyo_sayisi", request.banyoSayisi());
        input.put("mutfak_raw", safeString(request.mutfakRaw()));

        input.put("balkon", boolToInt(request.balkon()));
        input.put("asansor", boolToInt(request.asansor()));
        input.put("otopark", boolToInt(request.otopark()));
        input.put("esyali", boolToInt(request.esyali()));

        for (var direction : PredictionFeatureCatalog.YONLER) {
            input.put(direction.key(), selectedDirections.contains(direction.key()) ? 1 : 0);
        }

        input.put("guvenlik_skoru", countMatches(selectedFeatures, PredictionFeatureCatalog.getScoreKeys("guvenlik")));
        input.put("luks_skoru", countMatches(selectedFeatures, PredictionFeatureCatalog.getScoreKeys("luks_konfor")));
        input.put("sosyal_skoru",
                countMatches(selectedFeatures, PredictionFeatureCatalog.getScoreKeys("sosyal_tesis")));
        input.put("lokasyon_skoru",
                countMatches(selectedFeatures, PredictionFeatureCatalog.getScoreKeys("lokasyon_ulasim")));

        return input;
    }

    public Map<String, Object> buildPreview(PredictionRequest request) {
        Set<String> selectedFeatures = normalizeToSet(request.seciliOzellikler());

        Set<String> unknownKeys = new LinkedHashSet<>(selectedFeatures);
        unknownKeys.removeAll(PredictionFeatureCatalog.TUM_SECILEBILIR_OZELLIK_KEYLERI);

        LinkedHashMap<String, Object> preview = new LinkedHashMap<>();
        preview.put("modelInput", buildModelInput(request));
        preview.put("seciliOzellikSayisi", selectedFeatures.size());
        preview.put("bilinmeyenOzellikKeyleri", unknownKeys);
        preview.put("seciliGuvenlikKeyleri",
                intersect(selectedFeatures, PredictionFeatureCatalog.getScoreKeys("guvenlik")));
        preview.put("seciliLuksKeyleri",
                intersect(selectedFeatures, PredictionFeatureCatalog.getScoreKeys("luks_konfor")));
        preview.put("seciliSosyalKeyleri",
                intersect(selectedFeatures, PredictionFeatureCatalog.getScoreKeys("sosyal_tesis")));
        preview.put("seciliLokasyonKeyleri",
                intersect(selectedFeatures, PredictionFeatureCatalog.getScoreKeys("lokasyon_ulasim")));
        return preview;
    }

    private Set<String> normalizeToSet(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }

        Set<String> out = new LinkedHashSet<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                out.add(value.trim());
            }
        }
        return out;
    }

    private List<String> intersect(Set<String> selected, Set<String> targetGroup) {
        return selected.stream()
                .filter(targetGroup::contains)
                .sorted()
                .toList();
    }

    private int countMatches(Set<String> selected, Set<String> targetGroup) {
        int count = 0;
        for (String key : targetGroup) {
            if (selected.contains(key)) {
                count++;
            }
        }
        return count;
    }

    private int boolToInt(Boolean value) {
        return Boolean.TRUE.equals(value) ? 1 : 0;
    }

    private String safeString(String value) {
        return value == null ? "" : value.trim();
    }
}