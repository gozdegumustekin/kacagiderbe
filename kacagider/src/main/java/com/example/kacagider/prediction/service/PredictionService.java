package com.example.kacagider.prediction.service;

import com.example.kacagider.prediction.dto.PredictionRequest;
import com.example.kacagider.prediction.util.EmlakPreprocessor;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

@Service
public class PredictionService {

    private Classifier wekaModel;
    private Instances datasetStructure;

    // Uygulama ayağa kalktığında Modeli ve Şemayı RAM'e yükler
    @PostConstruct
    public void loadModel() {
        try {
            // 1. Zeki Beyni (RandomForest Modelini) Yükle
            InputStream modelStream = new ClassPathResource("emlak_rf_modeli.model").getInputStream();
            wekaModel = (Classifier) weka.core.SerializationHelper.read(modelStream);

            // 2. Tarif Defterini (ARFF Şemasını) Yükle
            // DİKKAT: resources klasöründe içi boşaltılmış (sadece @ATTRIBUTE kısımları
            // kalmış) bir schema.arff olmalı!
            InputStream schemaStream = new ClassPathResource("schema.arff").getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(schemaStream));
            datasetStructure = new Instances(reader);

            // Hedef değişkeni (fiyat_tl) en son sütun olarak ayarla
            datasetStructure.setClassIndex(datasetStructure.numAttributes() - 1);

            System.out.println(
                    "✅ Weka Modeli ve Şeması (" + datasetStructure.numAttributes() + " nitelik) başarıyla yüklendi!");

        } catch (Exception e) {
            System.err.println("❌ Model veya Şema yüklenirken hata oluştu: " + e.getMessage());
        }
    }

    public double predictPrice(PredictionRequest request) throws Exception {
        if (wekaModel == null || datasetStructure == null) {
            throw new Exception("Sistem henüz hazır değil, model yüklenemedi.");
        }

        // 1. YAMAK DEVREDE: Flutter'dan gelen metinleri (3+1, Kombi) al, matematiksel
        // sayılara çevir
        Map<String, Object> processedFeatures = EmlakPreprocessor.process(request.getFeatures());

        // Yeni bir Weka Örneği (Instance) oluştur
        Instance newInstance = new DenseInstance(datasetStructure.numAttributes());
        newInstance.setDataset(datasetStructure);

        // Önce tüm değerleri "Bilinmiyor (?)" olarak işaretle (Güvenlik için)
        for (int i = 0; i < datasetStructure.numAttributes(); i++) {
            newInstance.setMissing(i);
        }

        // 2. DİNAMİK EŞLEŞTİRME (177 Sütunu Otomatik Doldurma)
        for (int i = 0; i < datasetStructure.numAttributes(); i++) {
            Attribute attr = datasetStructure.attribute(i);
            String columnName = attr.name(); // Örn: "oda_sayisi", "Akilli_Ev"

            // Eğer Yamak'ın işlediği verilerde bu sütun varsa, değerini al ve Weka'ya ver
            if (processedFeatures.containsKey(columnName)) {
                Object value = processedFeatures.get(columnName);

                try {
                    if (attr.isNumeric()) {
                        // Sayısal değerleri ata (Örn: metrekare, oda sayısı)
                        newInstance.setValue(attr, Double.parseDouble(value.toString()));
                    } else if (attr.isNominal() && value != null) {
                        // Kategorik değerleri ata (Örn: İl, İlçe)
                        newInstance.setValue(attr, value.toString());
                    }
                } catch (IllegalArgumentException ex) {
                    // Eğer Flutter Weka'nın hiç bilmediği bir kelime gönderirse (Örn: İlçe="Paris")
                    // Çökme yaşanmasın diye o değeri "bilinmiyor" kategorisine çekiyoruz.
                    try {
                        newInstance.setValue(attr, "bilinmiyor");
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        // 3. BAŞ AŞÇI DEVREDE: Modeli çalıştır ve tahmini fiyatı döndür
        return wekaModel.classifyInstance(newInstance);
    }
}