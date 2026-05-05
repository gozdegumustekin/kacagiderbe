package com.example.kacagider.prediction.metadata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * pipeline_config.json'u Spring boot startup'ında yükler ve uygulamanın
 * her yerinden erişilebilir hale getirir.
 *
 * <p>Bu config dosyası Python tarafındaki feature_pipeline.py ile birebir
 * aynı dosyadır — yani modeli üretirken kullanılan kararlar runtime'da
 * tahmin yapılırken de aynen uygulanır.
 *
 * <p>Config'i değiştirmek istediğinde sadece JSON'u edit et, sonra uygulamayı
 * yeniden başlat. Yeni özellik mi çıktı? Bireyseli skora taşımak mı istiyorsun?
 * Hepsi JSON'da yapılır — kod değişmez.
 */
@Component
public class FeaturePipelineConfig {

    private static final String CONFIG_PATH = "pipeline_config.json";
    private static final String UNKNOWN = "bilinmiyor";

    private int version;
    private Map<String, List<String>> bireyselGruplari = Map.of();
    private Map<String, List<String>> skorGruplari = Map.of();
    private Map<String, Map<String, Double>> skorEsikleri = Map.of();
    private List<BinEntry> metrekareBinleri = List.of();
    private List<BinEntry> odaBinleri = List.of();
    private List<BinEntry> katSayisiBinleri = List.of();
    private List<BinEntry> banyoBinleri = List.of();
    private List<BinEntry> fiyatBinleri = List.of();
    private Map<String, String> fiyatEtiketTurkce = Map.of();

    // Cache'ler
    private List<String> tumBireyselListesi = List.of();
    private List<String> tumSkorOzellikleri = List.of();
    private Set<String> bireyselSet = Set.of();
    private Set<String> skorSet = Set.of();

    @PostConstruct
    public void load() {
        try (InputStream in = new ClassPathResource(CONFIG_PATH).getInputStream()) {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(in);

            this.version = root.path("version").asInt(0);

            this.bireyselGruplari = readListMap(root.path("bireysel_ozellikler"));
            this.skorGruplari = readListMap(root.path("skor_gruplari"));
            this.skorEsikleri = readEsikler(root.path("skor_esikleri"));

            this.metrekareBinleri = readBins(root.path("metrekare_binleri"));
            this.odaBinleri = readBins(root.path("oda_binleri"));
            this.katSayisiBinleri = readBins(root.path("kat_sayisi_binleri"));
            this.banyoBinleri = readBins(root.path("banyo_binleri"));
            this.fiyatBinleri = readBins(root.path("fiyat_binleri"));

            this.fiyatEtiketTurkce = readStringMap(root.path("fiyat_etiket_turkce"));

            // Cache
            LinkedHashSet<String> bireyselAll = new LinkedHashSet<>();
            for (List<String> g : bireyselGruplari.values()) bireyselAll.addAll(g);
            this.bireyselSet = Set.copyOf(bireyselAll);
            this.tumBireyselListesi = List.copyOf(bireyselAll);

            LinkedHashSet<String> skorAll = new LinkedHashSet<>();
            for (List<String> g : skorGruplari.values()) skorAll.addAll(g);
            this.skorSet = Set.copyOf(skorAll);
            this.tumSkorOzellikleri = List.copyOf(skorAll);

            System.out.println("✅ FeaturePipelineConfig v" + version + " yüklendi.");
            System.out.println("   • " + tumBireyselListesi.size() + " bireysel özellik ("
                    + bireyselGruplari.size() + " grup)");
            System.out.println("   • " + tumSkorOzellikleri.size() + " skor grup özelliği ("
                    + skorGruplari.size() + " grup)");

        } catch (Exception e) {
            throw new IllegalStateException(
                    "pipeline_config.json yüklenemedi (resources/ altında olmalı): "
                            + e.getMessage(), e);
        }
    }

    // ────────────────────────────────────────────────────────────
    //  GETTERS
    // ────────────────────────────────────────────────────────────
    public int getVersion() { return version; }
    public Map<String, List<String>> getBireyselGruplari() { return bireyselGruplari; }
    public Map<String, List<String>> getSkorGruplari() { return skorGruplari; }
    public List<String> getTumBireyselListesi() { return tumBireyselListesi; }
    public List<String> getTumSkorOzellikleri() { return tumSkorOzellikleri; }
    public Set<String> getBireyselSet() { return bireyselSet; }
    public Set<String> getSkorSet() { return skorSet; }

    public Map<String, String> getFiyatEtiketTurkce() { return fiyatEtiketTurkce; }

    public List<String> getSkorOzellikleri(String grupAdi) {
        return skorGruplari.getOrDefault(grupAdi, List.of());
    }

    public Map<String, Double> getSkorEsikleri(String grupAdi) {
        return skorEsikleri.getOrDefault(grupAdi, skorEsikleri.get("varsayilan"));
    }

    /**
     * Bir özelliğin ham CSV ismi (ör. "Hamam") hangi skor grubuna ait?
     * null = hiçbir gruba ait değil (bireysel veya bilinçli silinmiş olabilir).
     */
    public String skorGrubunuBul(String hamOzellikIsmi) {
        for (Map.Entry<String, List<String>> e : skorGruplari.entrySet()) {
            if (e.getValue().contains(hamOzellikIsmi)) return e.getKey();
        }
        return null;
    }

    public boolean bireyseldeMi(String hamOzellikIsmi) {
        return bireyselSet.contains(hamOzellikIsmi);
    }

    // ────────────────────────────────────────────────────────────
    //  BİNLEME FONKSİYONLARI — Python'daki _bin_lookup ile aynı
    // ────────────────────────────────────────────────────────────
    public String binMetrekare(Double val) { return binLookup(val, metrekareBinleri); }
    public String binKatSayisi(Integer val) {
        return binLookup(val == null ? null : val.doubleValue(), katSayisiBinleri);
    }
    public String binBanyoSayisi(Integer val) {
        return binLookup(val == null ? null : val.doubleValue(), banyoBinleri);
    }
    public String binOdaSayisi(Double val) { return binLookup(val, odaBinleri); }
    public String binFiyat(Double val) { return binLookup(val, fiyatBinleri); }

    private String binLookup(Double value, List<BinEntry> bins) {
        if (value == null || value <= 0 || bins.isEmpty()) return UNKNOWN;
        double v = value;
        for (BinEntry b : bins) {
            if (b.max == null || v < b.max) return b.etiket;
        }
        return bins.get(bins.size() - 1).etiket;
    }

    /** Bir skor count'unu nominal kategoriye çevirir (yok/cok dusuk/dusuk/orta/yuksek). */
    public String binSkor(int count, String grupAdi) {
        if (count <= 0) return "yok";
        Map<String, Double> esikler = getSkorEsikleri(grupAdi);
        double c = count;
        if (c <= esikler.getOrDefault("esik1", 0.5)) return "cok dusuk";
        if (c <= esikler.getOrDefault("esik2", 2.5)) return "dusuk";
        if (c <= esikler.getOrDefault("esik3", 5.5)) return "orta";
        return "yuksek";
    }

    // ────────────────────────────────────────────────────────────
    //  JSON YARDIMCILARI
    // ────────────────────────────────────────────────────────────
    private Map<String, List<String>> readListMap(JsonNode node) {
        LinkedHashMap<String, List<String>> out = new LinkedHashMap<>();
        if (node == null || !node.isObject()) return out;
        Iterator<String> fields = node.fieldNames();
        while (fields.hasNext()) {
            String key = fields.next();
            if (key.startsWith("_")) continue;
            JsonNode arr = node.get(key);
            if (!arr.isArray()) continue;
            List<String> vals = new ArrayList<>();
            arr.forEach(item -> vals.add(item.asText()));
            out.put(key, List.copyOf(vals));
        }
        return Map.copyOf(out);
    }

    private Map<String, Map<String, Double>> readEsikler(JsonNode node) {
        LinkedHashMap<String, Map<String, Double>> out = new LinkedHashMap<>();
        if (node == null || !node.isObject()) return out;
        Iterator<String> fields = node.fieldNames();
        while (fields.hasNext()) {
            String key = fields.next();
            if (key.startsWith("_")) continue;
            JsonNode obj = node.get(key);
            if (!obj.isObject()) continue;
            LinkedHashMap<String, Double> inner = new LinkedHashMap<>();
            Iterator<String> innerFields = obj.fieldNames();
            while (innerFields.hasNext()) {
                String k = innerFields.next();
                if (k.startsWith("_")) continue;
                inner.put(k, obj.get(k).asDouble());
            }
            out.put(key, Map.copyOf(inner));
        }
        return Map.copyOf(out);
    }

    private List<BinEntry> readBins(JsonNode node) {
        if (node == null || !node.isArray()) return List.of();
        List<BinEntry> out = new ArrayList<>();
        for (JsonNode b : node) {
            JsonNode maxNode = b.get("max");
            Double max = (maxNode == null || maxNode.isNull()) ? null : maxNode.asDouble();
            out.add(new BinEntry(max, b.get("etiket").asText()));
        }
        return List.copyOf(out);
    }

    private Map<String, String> readStringMap(JsonNode node) {
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        if (node == null || !node.isObject()) return out;
        Iterator<String> fields = node.fieldNames();
        while (fields.hasNext()) {
            String key = fields.next();
            if (key.startsWith("_")) continue;
            out.put(key, node.get(key).asText());
        }
        return Map.copyOf(out);
    }

    // ────────────────────────────────────────────────────────────
    //  IÇ SINIFLAR
    // ────────────────────────────────────────────────────────────
    public static record BinEntry(Double max, String etiket) {}
}
