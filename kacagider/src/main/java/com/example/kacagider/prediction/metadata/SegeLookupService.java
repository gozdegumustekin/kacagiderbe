package com.example.kacagider.prediction.metadata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * sege_kademe.json'u yükler ve il → SEGE kademesi sorgusu sunar.
 * Python tarafındaki feature_pipeline.py'ın SEGE lookup'ı ile aynı veridir.
 *
 * <p>Frontend SEGE göndermez; kullanıcı il seçince backend kademeyi
 * buradan bulur. Hem runtime feature (sege_kademe nominal) hem de
 * model routing (kademe → grup) için kullanılır.
 */
@Component
public class SegeLookupService {

    private static final String SEGE_PATH = "sege_kademe.json";

    /** il -> kademe (1..6) */
    private Map<String, Integer> kademeMap = Map.of();

    @PostConstruct
    public void load() {
        try (InputStream in = new ClassPathResource(SEGE_PATH).getInputStream()) {
            JsonNode root = new ObjectMapper().readTree(in);
            JsonNode kademeNode = root.path("kademe");

            LinkedHashMap<String, Integer> out = new LinkedHashMap<>();
            Iterator<String> iller = kademeNode.fieldNames();
            while (iller.hasNext()) {
                String il = iller.next();
                out.put(il.trim(), kademeNode.get(il).asInt());
            }
            this.kademeMap = Map.copyOf(out);

            System.out.println("✅ SegeLookupService yüklendi: " + kademeMap.size() + " il.");

        } catch (Exception e) {
            throw new IllegalStateException(
                    "sege_kademe.json okunamadı (resources/ altında olmalı): "
                            + e.getMessage(), e);
        }
    }

    /** İl adından SEGE kademe numarası (1..6). Bulunamazsa null. */
    public Integer getKademeNo(String il) {
        if (il == null) return null;
        return kademeMap.get(il.trim());
    }

    /**
     * İl adından nominal SEGE değeri: "kademe_3" gibi.
     * Bulunamazsa "bilinmiyor". ARFF feature olarak modele bu gider.
     */
    public String getKademeNominal(String il) {
        Integer k = getKademeNo(il);
        return k != null ? "kademe_" + k : "bilinmiyor";
    }

    public int ilSayisi() {
        return kademeMap.size();
    }
}
