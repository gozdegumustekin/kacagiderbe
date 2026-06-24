package com.example.kacagider.prediction.metadata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * pipeline_config.json'daki 'model_stratejisi' bloğunu okur.
 * Python tarafındaki model_strategy.py'ın Java karşılığıdır.
 *
 * <p>3 strateji: "tek", "kume3", "kademe6". Her strateji için grup tanımları
 * ({@link GrupBilgi}) tutulur: hangi SEGE kademeleri o gruba düşer, hangi
 * ARFF/model dosyası kullanılır.
 *
 * <p>il → kademe çözümü {@link SegeLookupService} üzerinden yapılır.
 */
@Component
public class ModelStrategyConfig {

    private static final String CONFIG_PATH = "pipeline_config.json";

    private final SegeLookupService segeLookup;

    private String aktifStrateji = "tek";
    /** strateji adı -> (grup adı -> GrupBilgi) */
    private Map<String, Map<String, GrupBilgi>> stratejiler = Map.of();

    @Autowired
    public ModelStrategyConfig(SegeLookupService segeLookup) {
        this.segeLookup = segeLookup;
    }

    @PostConstruct
    public void load() {
        try (InputStream in = new ClassPathResource(CONFIG_PATH).getInputStream()) {
            JsonNode root = new ObjectMapper().readTree(in);
            JsonNode blok = root.path("model_stratejisi");

            if (blok.isMissingNode()) {
                System.out.println("ℹ️  model_stratejisi bloğu yok; 'tek' varsayılıyor.");
                this.aktifStrateji = "tek";
                this.stratejiler = Map.of("tek", Map.of(
                        "hepsi", new GrupBilgi("hepsi", List.of(), "train_tek.arff", "model_tek.model")));
                return;
            }

            this.aktifStrateji = blok.path("aktif").asText("tek");

            LinkedHashMap<String, Map<String, GrupBilgi>> out = new LinkedHashMap<>();
            JsonNode stratNode = blok.path("stratejiler");
            Iterator<String> stratNames = stratNode.fieldNames();
            while (stratNames.hasNext()) {
                String stratAdi = stratNames.next();
                if (stratAdi.startsWith("_")) continue;
                JsonNode sNode = stratNode.get(stratAdi);

                LinkedHashMap<String, GrupBilgi> gruplar = new LinkedHashMap<>();

                if (stratAdi.equals("tek")) {
                    JsonNode md = sNode.path("model_dosyalari");
                    parseGruplar(md, gruplar);
                } else {
                    JsonNode g = sNode.path("gruplar");
                    parseGruplar(g, gruplar);
                }
                out.put(stratAdi, Map.copyOf(gruplar));
            }
            this.stratejiler = Map.copyOf(out);

            System.out.println("✅ ModelStrategyConfig yüklendi. Aktif: '" + aktifStrateji
                    + "', stratejiler: " + stratejiler.keySet());

        } catch (Exception e) {
            throw new IllegalStateException(
                    "model_stratejisi okunamadı: " + e.getMessage(), e);
        }
    }

    private void parseGruplar(JsonNode node, Map<String, GrupBilgi> hedef) {
        if (node == null || !node.isObject()) return;
        Iterator<String> names = node.fieldNames();
        while (names.hasNext()) {
            String grupAdi = names.next();
            if (grupAdi.startsWith("_")) continue;
            JsonNode g = node.get(grupAdi);

            List<Integer> kademeler = new ArrayList<>();
            JsonNode kArr = g.path("kademeler");
            if (kArr.isArray()) {
                kArr.forEach(k -> kademeler.add(k.asInt()));
            }
            String arff = g.path("arff").asText();
            String model = g.path("model").asText();
            hedef.put(grupAdi, new GrupBilgi(grupAdi, List.copyOf(kademeler), arff, model));
        }
    }

    // ────────────────────────────────────────────────────────────
    //  Sorgular
    // ────────────────────────────────────────────────────────────
    public String getAktifStrateji() {
        return aktifStrateji;
    }

    public boolean stratejiVarMi(String strateji) {
        return stratejiler.containsKey(strateji);
    }

    public List<String> tumStratejiler() {
        return List.copyOf(stratejiler.keySet());
    }

    public Map<String, GrupBilgi> gruplar(String strateji) {
        return stratejiler.getOrDefault(strateji, Map.of());
    }

    /**
     * Verilen il + strateji için hangi grup kullanılacak?
     * 'tek' stratejide her zaman "hepsi".
     * Diğerlerinde il→kademe→grup. SEGE'de bulunamayan il → null.
     */
    public GrupBilgi ilIcinGrup(String il, String strateji) {
        String s = (strateji == null || strateji.isBlank()) ? aktifStrateji : strateji;

        if (s.equals("tek")) {
            return gruplar("tek").get("hepsi");
        }

        Integer kademe = segeLookup.getKademeNo(il);  // 1..6 veya null
        if (kademe == null) return null;

        for (GrupBilgi g : gruplar(s).values()) {
            if (g.kademeler().contains(kademe)) {
                return g;
            }
        }
        return null;
    }

    /** Aktif stratejiyle ilIcinGrup. */
    public GrupBilgi ilIcinGrup(String il) {
        return ilIcinGrup(il, aktifStrateji);
    }

    // ────────────────────────────────────────────────────────────
    //  Grup bilgisi
    // ────────────────────────────────────────────────────────────
    public record GrupBilgi(
            String grupAdi,
            List<Integer> kademeler,
            String arffDosyasi,
            String modelDosyasi) {
    }
}
