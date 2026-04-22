package com.example.kacagider.prediction.service;

import com.example.kacagider.prediction.dto.*;
import com.example.kacagider.prediction.entity.PredictionImage;
import com.example.kacagider.prediction.entity.PredictionRecord;
import com.example.kacagider.prediction.repo.PredictionImageRepository;
import com.example.kacagider.prediction.repo.PredictionRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PredictionRecordService {

    private final PredictionRecordRepository predictionRecordRepository;
    private final PredictionImageRepository predictionImageRepository;
    private final PredictionService predictionService;
    private final PredictionInputBuilderService predictionInputBuilderService;
    private final FileStorageService fileStorageService;
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

    public List<PredictionImageResponse> attachImages(UUID userId, UUID predictionId, List<MultipartFile> images) {
        PredictionRecord record = predictionRecordRepository.findByIdAndUserId(predictionId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Kayıt bulunamadı."));

        if (images == null || images.isEmpty()) {
            return List.of();
        }

        long existingCount = predictionImageRepository.countByPredictionRecord_Id(predictionId);

        return java.util.stream.IntStream.range(0, images.size())
                .mapToObj(i -> {
                    MultipartFile file = images.get(i);
                    FileStorageService.StoredFileInfo stored = fileStorageService.storePredictionImage(predictionId,
                            file);

                    PredictionImage image = PredictionImage.builder()
                            .predictionRecord(record)
                            .storagePath(stored.relativePath())
                            .publicUrl(stored.publicUrl())
                            .originalFilename(stored.originalFilename())
                            .contentType(stored.contentType())
                            .sizeBytes(stored.sizeBytes())
                            .sortOrder((int) existingCount + i)
                            .resnetStatus("PENDING")
                            .build();

                    PredictionImage saved = predictionImageRepository.save(image);
                    return toResponse(saved);
                })
                .toList();
    }

    public List<PredictionImageResponse> getImages(UUID userId, UUID predictionId) {
        predictionRecordRepository.findByIdAndUserId(predictionId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Kayıt bulunamadı."));

        return predictionImageRepository.findAllByPredictionRecord_IdOrderBySortOrderAscCreatedAtAsc(predictionId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public void deleteImage(UUID userId, UUID predictionId, UUID imageId) {
        predictionRecordRepository.findByIdAndUserId(predictionId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Kayıt bulunamadı."));

        PredictionImage image = predictionImageRepository.findByIdAndPredictionRecord_Id(imageId, predictionId)
                .orElseThrow(() -> new IllegalArgumentException("Fotoğraf bulunamadı."));

        fileStorageService.deleteByRelativePath(image.getStoragePath());
        predictionImageRepository.delete(image);
    }

    private PredictionImageResponse toResponse(PredictionImage image) {
        return new PredictionImageResponse(
                image.getId(),
                image.getPredictionRecord().getId(),
                image.getOriginalFilename(),
                image.getContentType(),
                image.getSizeBytes(),
                image.getSortOrder(),
                image.getStoragePath(),
                image.getPublicUrl(),
                image.getResnetStatus(),
                image.getResnetLabel(),
                image.getResnetScore(),
                image.getCreatedAt());
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