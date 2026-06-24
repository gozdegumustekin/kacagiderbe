package com.example.kacagider.prediction.service;

import com.example.kacagider.prediction.dto.PredictionRequest;
import com.example.kacagider.prediction.dto.PredictionResponse;
import com.example.kacagider.prediction.metadata.FeaturePipelineConfig;
import com.example.kacagider.prediction.metadata.ModelStrategyConfig;
import com.example.kacagider.prediction.metadata.ModelStrategyConfig.GrupBilgi;
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

    private final PredictionInputBuilderService inputBuilder;
    private final FeaturePipelineConfig pipelineConfig;
    private final ModelStrategyConfig strategyConfig;

    @Value("${model.enabled:true}")
    private boolean modelEnabled;

    /** Yüklenen modeller: "strateji/grup" -> YukluModel */
    private final Map<String, YukluModel> modeller = new LinkedHashMap<>();

    @Autowired
    public PredictionService(PredictionInputBuilderService inputBuilder,
            FeaturePipelineConfig pipelineConfig,
            ModelStrategyConfig strategyConfig) {
        this.inputBuilder = inputBuilder;
        this.pipelineConfig = pipelineConfig;
        this.strategyConfig = strategyConfig;
    }

    @PostConstruct
    public void loadModels() {
        if (!modelEnabled) {
            System.out.println("ℹ️  model.enabled=false → modeller YÜKLENMEDİ.");
            return;
        }

        int basarili = 0, eksik = 0;
        for (String strateji : strategyConfig.tumStratejiler()) {
            for (GrupBilgi g : strategyConfig.gruplar(strateji).values()) {
                String anahtar = strateji + "/" + g.grupAdi();
                try {
                    // Config'teki model adına ".zip" ekleyerek ara (model_tek.model ->
                    // model_tek.model.zip)
                    String zipAdi = g.modelDosyasi() + ".zip";
                    ClassPathResource modelRes = new ClassPathResource(zipAdi);
                    ClassPathResource arffRes = new ClassPathResource(g.arffDosyasi());

                    if (!modelRes.exists() || !arffRes.exists()) {
                        System.out.println("  ⏭️  " + anahtar + " — model/arff yok ("
                                + zipAdi + " / " + g.arffDosyasi() + "), atlanıyor.");
                        eksik++;
                        continue;
                    }

                    Classifier clf;
                    try (InputStream rawStream = modelRes.getInputStream();
                            java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(rawStream)) {
                        java.util.zip.ZipEntry entry = zis.getNextEntry();
                        if (entry == null) {
                            throw new IllegalStateException("Model zip'i boş: " + zipAdi);
                        }
                        clf = (Classifier) weka.core.SerializationHelper.read(zis);
                    }

                    Instances structure;
                    try (InputStream as = arffRes.getInputStream();
                            BufferedReader br = new BufferedReader(new InputStreamReader(as))) {
                        structure = new Instances(br);
                    }
                    structure.setClassIndex(structure.numAttributes() - 1);

                    modeller.put(anahtar, new YukluModel(clf, structure));
                    basarili++;
                    System.out.println("  ✅ " + anahtar + " yüklendi ("
                            + structure.numAttributes() + " attr).");

                } catch (Exception e) {
                    System.out.println("  ⚠️  " + anahtar + " yüklenemedi: " + e.getMessage());
                    eksik++;
                }
            }
        }
        System.out.println("📦 Model yükleme bitti: " + basarili + " yüklendi, "
                + eksik + " eksik/atlandı. Aktif strateji: "
                + strategyConfig.getAktifStrateji());
    }

    public boolean stratejiHazirMi(String strateji) {
        String s = (strateji == null || strateji.isBlank())
                ? strategyConfig.getAktifStrateji()
                : strateji;
        return modeller.keySet().stream().anyMatch(k -> k.startsWith(s + "/"));
    }

    public boolean isModelHazir() {
        return !modeller.isEmpty();
    }

    public String getAktifStrateji() {
        return strategyConfig.getAktifStrateji();
    }

    /** Geriye dönük: aktif strateji, kalite yok. */
    public PredictionResponse predict(PredictionRequest request) throws Exception {
        return predict(request, null, null);
    }

    /** Kalite-farkında, aktif strateji (MlImageService bunu çağırır). */
    public PredictionResponse predict(PredictionRequest request,
            Map<String, String> odaKaliteleri) throws Exception {
        return predict(request, odaKaliteleri, null);
    }

    /**
     * Tam imza: kalite enjeksiyonu + strateji seçimi.
     * strateji null/boş ise aktif strateji kullanılır.
     */
    public PredictionResponse predict(PredictionRequest request,
            Map<String, String> odaKaliteleri,
            String strateji) throws Exception {

        if (!modelEnabled || modeller.isEmpty()) {
            throw new IllegalStateException(
                    "Tahmin modeli hazır değil. model.enabled ve resources/ altındaki "
                            + ".model.zip/.arff dosyalarını kontrol et.");
        }

        String s = (strateji == null || strateji.isBlank())
                ? strategyConfig.getAktifStrateji()
                : strateji;

        if (!strategyConfig.stratejiVarMi(s)) {
            throw new IllegalArgumentException("Bilinmeyen strateji: '" + s
                    + "'. Mevcut: " + strategyConfig.tumStratejiler());
        }

        GrupBilgi grup = strategyConfig.ilIcinGrup(request.il(), s);
        if (grup == null) {
            throw new IllegalArgumentException(
                    "'" + request.il() + "' ili için '" + s
                            + "' stratejisinde grup bulunamadı (SEGE'de yok olabilir).");
        }

        String anahtar = s + "/" + grup.grupAdi();
        YukluModel ym = modeller.get(anahtar);
        if (ym == null) {
            throw new IllegalStateException("Model yüklü değil: " + anahtar
                    + " (" + grup.modelDosyasi() + "). Weka'da eğitip resources/'a koy.");
        }

        // Input map (kalite dahil)
        Map<String, Object> features = inputBuilder.buildModelInput(request, odaKaliteleri);

        Instance inst = new DenseInstance(ym.structure.numAttributes());
        inst.setDataset(ym.structure);
        for (int i = 0; i < ym.structure.numAttributes(); i++) {
            inst.setMissing(i);
        }
        for (int i = 0; i < ym.structure.numAttributes(); i++) {
            Attribute attr = ym.structure.attribute(i);
            if (attr.index() == ym.structure.classIndex())
                continue;
            String col = attr.name();
            if (!features.containsKey(col))
                continue;
            Object value = features.get(col);
            if (value == null)
                continue;
            try {
                if (attr.isNumeric()) {
                    inst.setValue(attr, Double.parseDouble(value.toString()));
                } else if (attr.isNominal()) {
                    String nv = value.toString();
                    if (attr.indexOfValue(nv) >= 0)
                        inst.setValue(attr, nv);
                    else if (attr.indexOfValue("bilinmiyor") >= 0)
                        inst.setValue(attr, "bilinmiyor");
                    else if (attr.indexOfValue("yok") >= 0)
                        inst.setValue(attr, "yok");
                } else if (attr.isString()) {
                    inst.setValue(attr, value.toString());
                }
            } catch (Exception ignored) {
            }
        }

        double idx = ym.classifier.classifyInstance(inst);
        Attribute classAttr = ym.structure.classAttribute();
        String predictedLabel = classAttr.isNominal()
                ? classAttr.value((int) idx)
                : String.valueOf(idx);
        String displayText = formatPriceLabel(predictedLabel);

        Set<String> secili = onlySelected(request);
        Map<String, Integer> skorCounts = new LinkedHashMap<>();
        for (String grupAdi : pipelineConfig.getSkorGruplari().keySet()) {
            int c = 0;
            for (String oz : pipelineConfig.getSkorOzellikleri(grupAdi)) {
                if (secili.contains(oz))
                    c++;
            }
            skorCounts.put(grupAdi, c);
        }

        return new PredictionResponse(
                predictedLabel, displayText, skorCounts,
                "Tahmin '" + s + "' stratejisi, '" + grup.grupAdi() + "' modeliyle yapıldı.");
    }

    private Set<String> onlySelected(PredictionRequest request) {
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
        return pipelineConfig.getFiyatEtiketTurkce()
                .getOrDefault(label, label.replace("_", " "));
    }

    private static class YukluModel {
        final Classifier classifier;
        final Instances structure;

        YukluModel(Classifier classifier, Instances structure) {
            this.classifier = classifier;
            this.structure = structure;
        }
    }
}