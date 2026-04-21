package com.example.kacagider.prediction.service;

import com.example.kacagider.prediction.dto.*;
import com.example.kacagider.prediction.entity.PredictionRecord;
import com.example.kacagider.prediction.repo.PredictionRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PredictionRecordService {

    private final PredictionRecordRepository predictionRecordRepository;
    private final PredictionService predictionService;
    private final PredictionInputBuilderService predictionInputBuilderService;
    private final ObjectMapper objectMapper;

    public CreatePredictionRecordResponse create(UUID userId, PredictionRequest request) {
        validateRequest(request);

        try {
            Map<String, Object> preview = predictionInputBuilderService.buildPreview(request);
            PredictionResponse prediction = predictionService.predict(request);

            PredictionRecord record = PredictionRecord.builder()
                    .userId(userId)
                    .il(request.il())
                    .ilce(request.ilce())
                    .mahalle(request.mahalle())
                    .emlakTipi(request.emlakTipi())
                    .metrekareBrut(request.metrekareBrut())
                    .metrekareNet(request.metrekareNet())
                    .requestJson(objectMapper.valueToTree(request))
                    .previewJson(objectMapper.valueToTree(preview))
                    .predictionJson(objectMapper.valueToTree(prediction))
                    .build();

            PredictionRecord saved = predictionRecordRepository.save(record);

            return new CreatePredictionRecordResponse(
                    saved.getId(),
                    saved.getCreatedAt(),
                    prediction,
                    preview);

        } catch (Exception e) {
            throw new RuntimeException("Tahmin kaydı oluşturulurken hata oluştu: " + e.getMessage(), e);
        }
    }

    public List<PredictionHistoryItemResponse> getMyHistory(UUID userId) {
        return predictionRecordRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(record -> new PredictionHistoryItemResponse(
                        record.getId(),
                        record.getIl(),
                        record.getIlce(),
                        record.getMahalle(),
                        record.getEmlakTipi(),
                        record.getMetrekareBrut(),
                        record.getMetrekareNet(),
                        record.getPredictionJson(),
                        record.getCreatedAt()))
                .toList();
    }

    public PredictionDetailResponse getDetail(UUID userId, UUID predictionId) {
        PredictionRecord record = predictionRecordRepository.findByIdAndUserId(predictionId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Kayıt bulunamadı."));

        return new PredictionDetailResponse(
                record.getId(),
                record.getUserId(),
                record.getIl(),
                record.getIlce(),
                record.getMahalle(),
                record.getEmlakTipi(),
                record.getMetrekareBrut(),
                record.getMetrekareNet(),
                record.getRequestJson(),
                record.getPreviewJson(),
                record.getPredictionJson(),
                record.getCreatedAt());
    }

    private void validateRequest(PredictionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body boş olamaz.");
        }
        if (isBlank(request.il())) {
            throw new IllegalArgumentException("İl alanı zorunludur.");
        }
        if (isBlank(request.ilce())) {
            throw new IllegalArgumentException("İlçe alanı zorunludur.");
        }
        if (isBlank(request.emlakTipi())) {
            throw new IllegalArgumentException("Emlak tipi alanı zorunludur.");
        }
        if (request.metrekareBrut() == null || request.metrekareBrut() <= 0) {
            throw new IllegalArgumentException("Brüt metrekare 0'dan büyük olmalıdır.");
        }
        if (request.metrekareNet() == null || request.metrekareNet() <= 0) {
            throw new IllegalArgumentException("Net metrekare 0'dan büyük olmalıdır.");
        }
        if (request.katSayisi() == null || request.katSayisi() < 0) {
            throw new IllegalArgumentException("Kat sayısı negatif olamaz.");
        }
        if (request.banyoSayisi() == null || request.banyoSayisi() < 0) {
            throw new IllegalArgumentException("Banyo sayısı negatif olamaz.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}