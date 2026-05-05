package com.example.kacagider.prediction.service;

import com.example.kacagider.prediction.dto.PredictionRequest;
import com.example.kacagider.prediction.dto.PredictionResponse;
import com.example.kacagider.prediction.metadata.FeaturePipelineConfig;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Service
public class PredictionService {

    private final PredictionInputBuilderService predictionInputBuilderService;
    private final FeaturePipelineConfig pipelineConfig;

    /**
     * application.properties'teki model.enabled flag'i.
     * false → model yüklenmeye çalışılmaz, predict çağrıları
     * "model henüz hazır değil" hatası verir; uygulama yine ayağa kalkar.
     */
    @Value("${model.enabled:true}")
    private boolean modelEnabled;

    /**
     * application.properties'teki model.arff.path — varsayılan
     * train_emlak_hepsi.arff.
     * Eski model'i denemek istersen "train_emlak.arff" yapabilirsin.
     */
    @Value("${model.arff.path:train_emlak_hepsi.arff}")
    private String arffPath;

    @Value("${model.file.path:emlak_rf_modeli.model}")
    private String modelPath;

    private Classifier wekaModel;
    private Instances datasetStructure;
    private boolean modelHazir = false;

    @Autowired
    public PredictionService(PredictionInputBuilderService predictionInputBuilderService,
            FeaturePipelineConfig pipelineConfig) {
        this.predictionInputBuilderService = predictionInputBuilderService;
        this.pipelineConfig = pipelineConfig;
    }

    @PostConstruct
    public void loadModel() {
        if (!modelEnabled) {
            System.out.println("ℹ️  model.enabled=false → Weka modeli YÜKLENMEDİ. " +
                    "predict çağrıları reddedilecek; diğer endpoint'ler çalışır.");
            return;
        }

        try {
            // ARFF (şema) ve .model dosyaları classpath'te (resources/ altında) olmalı
            ClassPathResource modelResource = new ClassPathResource(modelPath);
            ClassPathResource arffResource = new ClassPathResource(arffPath);

            if (!modelResource.exists()) {
                System.err.println("⚠️  Model dosyası bulunamadı: " + modelPath +
                        " — predict endpoint'i devre dışı, diğer endpoint'ler çalışır.");
                return;
            }
            if (!arffResource.exists()) {
                System.err.println("⚠️  ARFF dosyası bulunamadı: " + arffPath +
                        " — predict endpoint'i devre dışı, diğer endpoint'ler çalışır.");
                return;
            }

            try (InputStream modelStream = modelResource.getInputStream()) {
                wekaModel = (Classifier) weka.core.SerializationHelper.read(modelStream);
            }

            try (InputStream schemaStream = arffResource.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(schemaStream))) {
                datasetStructure = new Instances(reader);
            }

            datasetStructure.setClassIndex(datasetStructure.numAttributes() - 1);
            modelHazir = true;

            System.out.println("✅ Weka modeli ve " + arffPath + " başarıyla yüklendi.");
            System.out.println("   Class attribute: " + datasetStructure.classAttribute().name());
            System.out.println("   Toplam attribute: " + datasetStructure.numAttributes());
            System.out.println("   Pipeline config v" + pipelineConfig.getVersion());

        } catch (Exception e) {
            System.err.println("⚠️  Model veya ARFF yüklenirken hata: " + e.getMessage() +
                    " — predict endpoint'i devre dışı, diğer endpoint'ler çalışır.");
            modelHazir = false;
        }
    }

    /**
     * predict endpoint'inin kullanılabilir olup olmadığı — health check'te de
     * gösterilebilir.
     */
    public boolean isModelHazir() {
        return modelHazir;
    }

    public PredictionResponse predict(PredictionRequest request) throws Exception {
        if (!modelHazir) {
            throw new IllegalStateException(
                    "Tahmin modeli henüz hazır değil. Model dosyası ve ARFF " +
                            "src/main/resources/ altında olmalı. " +
                            "Şu an aranıyor: " + modelPath + " / " + arffPath);
        }

        Map<String, Object> processedFeatures = predictionInputBuilderService.buildModelInput(request);

        Instance newInstance = new DenseInstance(datasetStructure.numAttributes());
        newInstance.setDataset(datasetStructure);

        for (int i = 0; i < datasetStructure.numAttributes(); i++) {
            newInstance.setMissing(i);
        }

        for (int i = 0; i < datasetStructure.numAttributes(); i++) {
            Attribute attr = datasetStructure.attribute(i);
            if (attr.index() == datasetStructure.classIndex())
                continue;

            String columnName = attr.name();
            if (!processedFeatures.containsKey(columnName))
                continue;

            Object value = processedFeatures.get(columnName);
            try {
                if (value == null)
                    continue;

                if (attr.isNumeric()) {
                    newInstance.setValue(attr, Double.parseDouble(value.toString()));
                } else if (attr.isNominal()) {
                    String nominalValue = value.toString();
                    if (attr.indexOfValue(nominalValue) >= 0) {
                        newInstance.setValue(attr, nominalValue);
                    } else if (attr.indexOfValue("bilinmiyor") >= 0) {
                        newInstance.setValue(attr, "bilinmiyor");
                    } else if (attr.indexOfValue("yok") >= 0) {
                        newInstance.setValue(attr, "yok");
                    }
                } else if (attr.isString()) {
                    newInstance.setValue(attr, value.toString());
                }
            } catch (Exception ignored) {
            }
        }

        double predictionIndex = wekaModel.classifyInstance(newInstance);
        Attribute classAttr = datasetStructure.classAttribute();
        String predictedLabel = classAttr.isNominal()
                ? classAttr.value((int) predictionIndex)
                : String.valueOf(predictionIndex);

        String displayText = formatPriceLabel(predictedLabel);

        Set<String> seciliHamIsimler = onlySelectedHamIsimler(request);
        Map<String, Integer> skorCounts = new LinkedHashMap<>();
        for (String grupAdi : pipelineConfig.getSkorGruplari().keySet()) {
            int count = 0;
            for (String ozellik : pipelineConfig.getSkorOzellikleri(grupAdi)) {
                if (seciliHamIsimler.contains(ozellik))
                    count++;
            }
            skorCounts.put(grupAdi, count);
        }

        return new PredictionResponse(
                predictedLabel,
                displayText,
                skorCounts,
                "Tahmini fiyat aralığı başarıyla hesaplandı.");
    }

    private Set<String> onlySelectedHamIsimler(PredictionRequest request) {
        Map<String, Boolean> map = request.ozelliklerOrEmpty();
        Set<String> out = new LinkedHashSet<>();
        for (var e : map.entrySet()) {
            if (Boolean.TRUE.equals(e.getValue()) && e.getKey() != null && !e.getKey().isBlank()) {
                out.add(e.getKey().trim());
            }
        }
        return out;
    }

    private String formatPriceLabel(String label) {
        if (label == null || label.isBlank())
            return "Bilinmiyor";
        Map<String, String> sozluk = pipelineConfig.getFiyatEtiketTurkce();
        return sozluk.getOrDefault(label, label.replace("_", " "));
    }
}