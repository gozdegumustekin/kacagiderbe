package com.example.kacagider.prediction.service;

import com.example.kacagider.prediction.entity.PredictionImage;
import com.example.kacagider.prediction.entity.PredictionRecord;
import com.example.kacagider.prediction.repo.PredictionImageRepository;
import com.example.kacagider.prediction.repo.PredictionRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * FastAPI ML servisinden gelen DOĞRULANMIŞ fotoğrafları ve kalite
 * sonuçlarını işler. Normal kullanıcı upload'ından (attachImages) ayrıdır:
 * burada fotoğraf zaten 4 aşamadan geçmiş ve kalite belirlenmiştir.
 *
 * <p>
 * Oda başına birden çok fotoğraf olabilir; tabular tahmin için
 * {@link #odaKaliteOrtalamalari(UUID)} ile oda tipine göre kalite ortalaması
 * hesaplanır.
 */
@Service
@RequiredArgsConstructor
public class MlImageService {

    private final PredictionRecordRepository predictionRecordRepository;
    private final PredictionImageRepository predictionImageRepository;
    private final FileStorageService fileStorageService;

    // Kalite etiketi → ordinal (ortalama almak için). Python ile aynı.
    private static final Map<String, Double> KALITE_ORDINAL = Map.of(
            "Kotu", 0.0, "Normal", 1.0, "Iyi", 2.0);

    /**
     * FastAPI'nin ilettiği doğrulanmış fotoğrafı saklar + kalite ile kaydeder.
     *
     * @param userId       JWT'den çözülen kullanıcı (yetki kontrolü)
     * @param predictionId hangi tahmin kaydına ait
     * @param file         fotoğraf
     * @param odaTipi      salon/oda/mutfak/banyo
     * @param kaliteEtiket Kotu/Normal/Iyi
     * @param kaliteSkor   ordinal skor (0-2)
     * @param kaliteGuven  model güveni (0-1)
     */
    public PredictionImage kaydetMlFotograf(
            UUID userId, UUID predictionId, MultipartFile file,
            String odaTipi, String kaliteEtiket, Double kaliteSkor, Double kaliteGuven) {

        PredictionRecord record = predictionRecordRepository
                .findByIdAndUserId(predictionId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Kayıt bulunamadı."));

        FileStorageService.StoredFileInfo stored = fileStorageService.storePredictionImage(predictionId, file);

        long mevcut = predictionImageRepository.countByPredictionRecord_Id(predictionId);

        PredictionImage image = PredictionImage.builder()
                .predictionRecord(record)
                .storagePath(stored.relativePath())
                .publicUrl(stored.publicUrl())
                .originalFilename(stored.originalFilename())
                .contentType(stored.contentType())
                .sizeBytes(stored.sizeBytes())
                .sortOrder((int) mevcut)
                .odaTipi(odaTipi)
                .resnetStatus("DONE") // ML servisi zaten işledi
                .resnetLabel(kaliteEtiket)
                .resnetScore(kaliteGuven) // güveni score olarak tut
                .build();

        return predictionImageRepository.save(image);
    }

    /**
     * Bir tahmin kaydındaki tüm fotoğrafları oda tipine göre gruplayıp
     * her oda için kalite ortalamasını döndürür.
     *
     * <p>
     * Tabular tahminde feature olarak kullanılır:
     * salon_kalitesi, oda_kalitesi, mutfak_kalitesi, banyo_kalitesi
     *
     * @return {"salon": "Iyi", "banyo": "Normal", ...} (sadece foto olan odalar)
     */
    public Map<String, String> odaKaliteOrtalamalari(UUID predictionId) {
        List<PredictionImage> hepsi = predictionImageRepository
                .findAllByPredictionRecord_IdOrderBySortOrderAscCreatedAtAsc(predictionId);

        // oda tipi -> ordinal skorların toplamı + adet
        Map<String, double[]> birikim = new LinkedHashMap<>(); // [toplam, adet]

        for (PredictionImage img : hepsi) {
            if (img.getOdaTipi() == null || img.getResnetLabel() == null)
                continue;
            Double ord = KALITE_ORDINAL.get(img.getResnetLabel());
            if (ord == null)
                continue;

            birikim.computeIfAbsent(img.getOdaTipi(), k -> new double[] { 0.0, 0.0 });
            double[] acc = birikim.get(img.getOdaTipi());
            acc[0] += ord;
            acc[1] += 1;
        }

        Map<String, String> sonuc = new LinkedHashMap<>();
        for (var e : birikim.entrySet()) {
            double ort = e.getValue()[0] / e.getValue()[1];
            sonuc.put(e.getKey(), ordinalToEtiket(ort));
        }
        return sonuc;
    }

    private String ordinalToEtiket(double ort) {
        int y = (int) Math.round(ort);
        y = Math.max(0, Math.min(2, y));
        return switch (y) {
            case 0 -> "Kotu";
            case 2 -> "Iyi";
            default -> "Normal";
        };
    }

    /**
     * Tabular tahmin (ARFF) için oda kaliteleri — KÜÇÜK HARF değerlerle.
     *
     * <p>
     * ARFF'teki image feature attribute'ları {kotu,normal,iyi,luks} domain'ini
     * kullanır (küçük harf). ML servisi "Iyi/Normal/Kotu" (büyük harf) döndürür,
     * bu metod onları ARFF'e uygun küçük harfe çevirir.
     *
     * @return {"salon_kalitesi": "iyi", "mutfak_kalitesi": "normal", ...}
     *         Sadece foto yüklenmiş odalar döner. ARFF attribute adlarıyla
     *         (oda + "_kalitesi") anahtarlanır.
     */
    public Map<String, String> arffKaliteFeatureleri(UUID predictionId) {
        Map<String, String> ortalamalar = odaKaliteOrtalamalari(predictionId);
        Map<String, String> arff = new LinkedHashMap<>();
        for (var e : ortalamalar.entrySet()) {
            String odaTipi = e.getKey(); // salon / mutfak / banyo
            String etiketKucuk = e.getValue().toLowerCase(); // Iyi → iyi
            arff.put(odaTipi + "_kalitesi", etiketKucuk); // salon_kalitesi = iyi
        }
        return arff;
    }
}
