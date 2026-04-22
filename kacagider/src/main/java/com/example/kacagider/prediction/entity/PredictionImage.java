package com.example.kacagider.prediction.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "prediction_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PredictionImage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prediction_record_id", nullable = false)
    private PredictionRecord predictionRecord;

    @Column(nullable = false)
    private String storagePath;

    @Column(nullable = false)
    private String publicUrl;

    @Column(nullable = false)
    private String originalFilename;

    private String contentType;

    @Column(nullable = false)
    private Long sizeBytes;

    @Column(nullable = false)
    private Integer sortOrder;

    @Column(nullable = false)
    private String resnetStatus; // PENDING, DONE, FAILED

    private String resnetLabel; // kötü, normal, iyi, lüks vb.

    private Double resnetScore;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (resnetStatus == null || resnetStatus.isBlank()) {
            resnetStatus = "PENDING";
        }
        if (sortOrder == null) {
            sortOrder = 0;
        }
    }
}