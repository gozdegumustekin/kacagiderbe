package com.example.kacagider.prediction.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path uploadRoot;
    private final String publicPrefix;
    private final String publicBaseUrl;

    public FileStorageService(
            @Value("${app.upload.root}") String uploadRoot,
            @Value("${app.upload.public-prefix:/uploads}") String publicPrefix,
            @Value("${app.public-base-url:http://localhost:8080}") String publicBaseUrl) {
        this.uploadRoot = Paths.get(uploadRoot).toAbsolutePath().normalize();
        this.publicPrefix = normalizePrefix(publicPrefix);
        this.publicBaseUrl = stripTrailingSlash(publicBaseUrl);
    }

    public StoredFileInfo storePredictionImage(UUID predictionRecordId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Boş dosya yüklenemez.");
        }

        try {
            String originalFilename = file.getOriginalFilename() == null
                    ? "image"
                    : Paths.get(file.getOriginalFilename()).getFileName().toString();

            String extension = extractExtension(originalFilename);
            String generatedFilename = UUID.randomUUID() + extension;

            Path targetDir = uploadRoot.resolve(Paths.get("predictions", predictionRecordId.toString())).normalize();
            Files.createDirectories(targetDir);

            Path targetFile = targetDir.resolve(generatedFilename).normalize();

            if (!targetFile.startsWith(uploadRoot)) {
                throw new IllegalArgumentException("Geçersiz dosya yolu.");
            }

            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);

            String relativePath = uploadRoot.relativize(targetFile).toString().replace("\\", "/");
            String publicUrl = publicBaseUrl + publicPrefix + "/" + relativePath;

            return new StoredFileInfo(
                    relativePath,
                    publicUrl,
                    originalFilename,
                    file.getContentType(),
                    file.getSize());
        } catch (IOException e) {
            throw new RuntimeException("Dosya kaydedilemedi: " + e.getMessage(), e);
        }
    }

    public void deleteByRelativePath(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }

        try {
            Path target = uploadRoot.resolve(relativePath).normalize();

            if (!target.startsWith(uploadRoot)) {
                throw new IllegalArgumentException("Geçersiz dosya yolu.");
            }

            Files.deleteIfExists(target);
        } catch (IOException e) {
            throw new RuntimeException("Dosya silinemedi: " + e.getMessage(), e);
        }
    }

    private String extractExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        if (idx == -1 || idx == filename.length() - 1) {
            return "";
        }
        return filename.substring(idx);
    }

    private String normalizePrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return "/uploads";
        }
        String p = prefix.trim();
        if (!p.startsWith("/")) {
            p = "/" + p;
        }
        return stripTrailingSlash(p);
    }

    private String stripTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    public record StoredFileInfo(
            String relativePath,
            String publicUrl,
            String originalFilename,
            String contentType,
            long sizeBytes) {
    }
}