package com.example.kacagider.prediction.repo;

import com.example.kacagider.prediction.entity.PredictionRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PredictionRecordRepository extends JpaRepository<PredictionRecord, UUID> {

    List<PredictionRecord> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<PredictionRecord> findByIdAndUserId(UUID id, UUID userId);
}