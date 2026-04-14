package com.example.kacagider.prediction.dto;

import java.util.List;
import java.util.Map;

public record FormMetadataResponse(
        List<FormFieldDto> temelAlanlar,
        List<FeatureItemDto> temelBinaryAlanlar,
        List<FeatureItemDto> yonler,
        Map<String, List<FeatureItemDto>> skorGruplari,
        Map<String, List<FeatureItemDto>> digerOzellikGruplari) {
}