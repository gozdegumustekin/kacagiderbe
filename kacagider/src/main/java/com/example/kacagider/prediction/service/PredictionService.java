package com.example.kacagider.prediction.service;

import com.example.kacagider.prediction.dto.PredictionRequest;
import com.example.kacagider.prediction.dto.PredictionResponse;
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

    private final PredictionInputBuilderService predictionInputBuilderService;

    private Classifier wekaModel;
    private Instances datasetStructure;

    public PredictionService(PredictionInputBuilderService predictionInputBuilderService) {
        this.predictionInputBuilderService = predictionInputBuilderService;
    }

    @PostConstruct
    public void loadModel() {
        try {
            InputStream modelStream = new ClassPathResource("emlak_rf_modeli.model").getInputStream();
            wekaModel = (Classifier) weka.core.SerializationHelper.read(modelStream);

            // schema.arff yerine train_emlak.arff yükleniyor
            InputStream schemaStream = new ClassPathResource("train_emlak.arff").getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(schemaStream));
            datasetStructure = new Instances(reader);

            datasetStructure.setClassIndex(datasetStructure.numAttributes() - 1);

            System.out.println("✅ Weka modeli ve train_emlak.arff başarıyla yüklendi.");
            System.out.println("Class attribute: " + datasetStructure.classAttribute().name());

        } catch (Exception e) {
            System.err.println("❌ Model veya ARFF yüklenemedi: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Prediction model yüklenemedi.", e);
        }
    }

    public PredictionResponse predict(PredictionRequest request) throws Exception {
        if (wekaModel == null || datasetStructure == null) {
            throw new Exception("Sistem hazır değil. Model veya ARFF yüklenemedi.");
        }

        Map<String, Object> processedFeatures = predictionInputBuilderService.buildModelInput(request);

        Instance newInstance = new DenseInstance(datasetStructure.numAttributes());
        newInstance.setDataset(datasetStructure);

        for (int i = 0; i < datasetStructure.numAttributes(); i++) {
            newInstance.setMissing(i);
        }

        for (int i = 0; i < datasetStructure.numAttributes(); i++) {
            Attribute attr = datasetStructure.attribute(i);

            if (attr.index() == datasetStructure.classIndex()) {
                continue;
            }

            String columnName = attr.name();

            if (!processedFeatures.containsKey(columnName)) {
                continue;
            }

            Object value = processedFeatures.get(columnName);

            try {
                if (value == null) {
                    continue;
                }

                if (attr.isNumeric()) {
                    newInstance.setValue(attr, Double.parseDouble(value.toString()));
                } else if (attr.isNominal()) {
                    String nominalValue = value.toString();

                    if (attr.indexOfValue(nominalValue) >= 0) {
                        newInstance.setValue(attr, nominalValue);
                    } else if (attr.indexOfValue("bilinmiyor") >= 0) {
                        newInstance.setValue(attr, "bilinmiyor");
                    }
                } else if (attr.isString()) {
                    newInstance.setValue(attr, value.toString());
                }

            } catch (Exception ignored) {
            }
        }

        double predictionIndex = wekaModel.classifyInstance(newInstance);

        Attribute classAttr = datasetStructure.classAttribute();
        String predictedLabel;

        if (classAttr.isNominal()) {
            predictedLabel = classAttr.value((int) predictionIndex);
        } else {
            predictedLabel = String.valueOf(predictionIndex);
        }

        String displayText = formatPriceLabel(predictedLabel);

        Integer guvenlikSkoru = toInt(processedFeatures.get("guvenlik_skoru"));
        Integer luksSkoru = toInt(processedFeatures.get("luks_skoru"));
        Integer sosyalSkoru = toInt(processedFeatures.get("sosyal_skoru"));
        Integer lokasyonSkoru = toInt(processedFeatures.get("lokasyon_skoru"));

        return new PredictionResponse(
                predictedLabel,
                displayText,
                guvenlikSkoru,
                luksSkoru,
                sosyalSkoru,
                lokasyonSkoru,
                "Tahmini fiyat aralığı başarıyla hesaplandı.");
    }

    private Integer toInt(Object value) {
        if (value == null) {
            return 0;
        }
        try {
            return (int) Math.round(Double.parseDouble(value.toString()));
        } catch (Exception e) {
            return 0;
        }
    }

    private String formatPriceLabel(String label) {
        if (label == null || label.isBlank()) {
            return "Bilinmiyor";
        }

        return switch (label) {
            case "500_Bin_TL_Alti" -> "500 Bin TL altı";
            case "500_Bin_1_Milyon_TL_Arasi" -> "500 Bin TL - 1 Milyon TL";
            case "1_1_Bucuk_Milyon_TL_Arasi" -> "1 Milyon TL - 1.5 Milyon TL";
            case "1_Bucuk_2_Milyon_TL_Arasi" -> "1.5 Milyon TL - 2 Milyon TL";
            case "2_3_Milyon_TL_Arasi" -> "2 Milyon TL - 3 Milyon TL";
            case "3_4_Milyon_TL_Arasi" -> "3 Milyon TL - 4 Milyon TL";
            case "4_5_Milyon_TL_Arasi" -> "4 Milyon TL - 5 Milyon TL";
            case "5_7_Bucuk_Milyon_TL_Arasi" -> "5 Milyon TL - 7.5 Milyon TL";
            case "7_Bucuk_10_Milyon_TL_Arasi" -> "7.5 Milyon TL - 10 Milyon TL";
            case "10_15_Milyon_TL_Arasi" -> "10 Milyon TL - 15 Milyon TL";
            case "15_25_Milyon_TL_Arasi" -> "15 Milyon TL - 25 Milyon TL";
            case "25_50_Milyon_TL_Arasi" -> "25 Milyon TL - 50 Milyon TL";
            case "50_100_Milyon_TL_Arasi" -> "50 Milyon TL - 100 Milyon TL";
            case "100_250_Milyon_TL_Arasi" -> "100 Milyon TL - 250 Milyon TL";
            case "250_500_Milyon_TL_Arasi" -> "250 Milyon TL - 500 Milyon TL";
            case "500_Milyon_1_Milyar_TL_Arasi" -> "500 Milyon TL - 1 Milyar TL";
            case "1_2_Milyar_TL_Arasi" -> "1 Milyar TL - 2 Milyar TL";
            case "2_4_Milyar_TL_Arasi" -> "2 Milyar TL - 4 Milyar TL";
            case "4_6_Milyar_TL_Arasi" -> "4 Milyar TL - 6 Milyar TL";
            case "6_Milyar_TL_Ustu" -> "6 Milyar TL ve üzeri";
            default -> label.replace("_", " ");
        };
    }
}