package com.example.kacagider.prediction.repo;

import com.example.kacagider.prediction.entity.PredictionImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PredictionImageRepository extends JpaRepository<PredictionImage, UUID> {

    List<PredictionImage> findAllByPredictionRecord_IdOrderBySortOrderAscCreatedAtAsc(UUID predictionRecordId);

    Optional<PredictionImage> findByIdAndPredictionRecord_Id(UUID imageId, UUID predictionRecordId);

    long countByPredictionRecord_Id(UUID predictionRecordId);
}