package com.example.kacagider.prediction.service;

import com.example.kacagider.prediction.dto.PredictionRequest;
import com.example.kacagider.prediction.metadata.FeaturePipelineConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Frontend'den gelen ham {@link PredictionRequest}'i, ARFF nominal şemasıyla
 * birebir uyumlu bir Map'e çevirir. Tüm pipeline kararları
 * {@link FeaturePipelineConfig}'ten gelir.
 *
 * <p>
 * Çıktı Map'inin anahtarları, csv_to_arff_hepsi.py tarafından üretilen
 * ARFF dosyasındaki @ATTRIBUTE adlarıyla birebir aynıdır:
 * <ul>
 * <li>il, ilce, mahalle (kategorik, temizlenmiş)</li>
 * <li>emlak_tipi, bina_yasi_raw, bulundugu_kat_raw, isitma_ana_sinif,
 * mutfak_raw</li>
 * <li>metrekare_kategori, oda_kategori, kat_sayisi_kategori,
 * banyo_kategori</li>
 * <li>balkon, asansor, otopark, esyali, mutfak_acik_mi (yok/var)</li>
 * <li>Bireysel binary'ler (Bati, Dogu, ..., Hamam, Akilli_Ev, ... — clean
 * isimler) (yok/var)</li>
 * <li>Skor kategorileri: guvenlik_kategori, luks_kategori, ... (nominal)</li>
 * </ul>
 */
@Service
public class PredictionInputBuilderService {

    private static final String UNKNOWN = "bilinmiyor";

    private final FeaturePipelineConfig pipelineConfig;

    @Autowired
    public PredictionInputBuilderService(FeaturePipelineConfig pipelineConfig) {
        this.pipelineConfig = pipelineConfig;
    }

    public Map<String, Object> buildModelInput(PredictionRequest request) {
        // Kullanıcının seçtiği TÜM özellikler (Map<String, Boolean>) — ham isim →
        // seçildi mi
        Map<String, Boolean> tumOzellikler = request.ozelliklerOrEmpty();
        Set<String> seciliHamIsimler = onlySelected(tumOzellikler);

        LinkedHashMap<String, Object> input = new LinkedHashMap<>();

        // ── Lokasyon ──
        input.put("il", cleanNominal(safeString(request.il())));
        input.put("ilce", cleanNominal(safeString(request.ilce())));
        input.put("mahalle", cleanNominal(safeString(request.mahalle())));

        // ── Diğer kategorik ──
        input.put("emlak_tipi", cleanNominal(safeString(request.emlakTipi())));
        input.put("bina_yasi_raw", cleanNominal(safeString(request.binaYasiRaw())));
        input.put("bulundugu_kat_raw", cleanNominal(safeString(request.bulunduguKatRaw())));
        input.put("isitma_ana_sinif", cleanNominal(isitmaAnaSinif(request.isitmaRaw())));
        input.put("mutfak_raw", cleanNominal(safeString(request.mutfakRaw())));

        // ── Numeric → Nominal binning (config'ten) ──
        input.put("metrekare_kategori", cleanNominal(pipelineConfig.binMetrekare(request.metrekareBrut())));
        input.put("oda_kategori", cleanNominal(pipelineConfig.binOdaSayisi(parseOdaSayisi(request.odaSayisiRaw()))));
        input.put("kat_sayisi_kategori", cleanNominal(pipelineConfig.binKatSayisi(request.katSayisi())));
        input.put("banyo_kategori", cleanNominal(pipelineConfig.binBanyoSayisi(request.banyoSayisi())));

        // ── Temel binary ──
        input.put("balkon", boolToYokVar(request.balkon()));
        input.put("asansor", boolToYokVar(request.asansor()));
        input.put("otopark", boolToYokVar(request.otopark()));
        input.put("esyali", boolToYokVar(request.esyali()));
        input.put("mutfak_acik_mi", mutfakAcikYokVar(request.mutfakRaw()));

        // ── BIREYSEL BINARY'LER (config'ten okunur) ──
        // Her bireysel özelliğin ARFF attribute adı = clean(ham isim)
        for (String hamIsim : pipelineConfig.getTumBireyselListesi()) {
            String cleanKey = cleanAttrName(hamIsim);
            input.put(cleanKey, seciliHamIsimler.contains(hamIsim) ? "var" : "yok");
        }

        // ── SKOR GRUPLARI → nominal kategori (config'ten okunur) ──
        for (String grupAdi : pipelineConfig.getSkorGruplari().keySet()) {
            int count = countMatches(seciliHamIsimler, pipelineConfig.getSkorOzellikleri(grupAdi));
            String kategori = pipelineConfig.binSkor(count, grupAdi);
            input.put(grupAdi + "_kategori", kategori);
        }

        return input;
    }

    /** Frontend için — pipeline'ın ne yaptığını gösteren önizleme. */
    public Map<String, Object> buildPreview(PredictionRequest request) {
        Map<String, Boolean> tumOzellikler = request.ozelliklerOrEmpty();
        Set<String> seciliHamIsimler = onlySelected(tumOzellikler);

        // Bilinmeyen özellikler — UI'da olmayan, ama frontend'in göndermeyi denediği
        Set<String> tumBilinen = new LinkedHashSet<>(pipelineConfig.getTumBireyselListesi());
        tumBilinen.addAll(pipelineConfig.getTumSkorOzellikleri());
        Set<String> bilinmeyen = new LinkedHashSet<>(seciliHamIsimler);
        bilinmeyen.removeAll(tumBilinen);

        LinkedHashMap<String, Object> preview = new LinkedHashMap<>();
        preview.put("modelInput", buildModelInput(request));
        preview.put("seciliOzellikSayisi", seciliHamIsimler.size());
        preview.put("bilinmeyenOzellikleri", bilinmeyen);
        preview.put("pipelineVersion", pipelineConfig.getVersion());

        // Skor grup detayları
        LinkedHashMap<String, Object> skorlar = new LinkedHashMap<>();
        for (String grupAdi : pipelineConfig.getSkorGruplari().keySet()) {
            List<String> grupOzellikleri = pipelineConfig.getSkorOzellikleri(grupAdi);
            List<String> seciliOlanlar = grupOzellikleri.stream()
                    .filter(seciliHamIsimler::contains)
                    .sorted()
                    .toList();
            LinkedHashMap<String, Object> bilgi = new LinkedHashMap<>();
            bilgi.put("count", seciliOlanlar.size());
            bilgi.put("seciliOzellikler", seciliOlanlar);
            bilgi.put("kategori", pipelineConfig.binSkor(seciliOlanlar.size(), grupAdi));
            skorlar.put(grupAdi, bilgi);
        }
        preview.put("skorDetaylari", skorlar);

        // Bireysel olarak seçilenler
        LinkedHashMap<String, List<String>> bireyselSecimler = new LinkedHashMap<>();
        for (var entry : pipelineConfig.getBireyselGruplari().entrySet()) {
            List<String> secilen = entry.getValue().stream()
                    .filter(seciliHamIsimler::contains)
                    .sorted()
                    .toList();
            bireyselSecimler.put(entry.getKey(), secilen);
        }
        preview.put("bireyselSecimler", bireyselSecimler);

        return preview;
    }

    // ────────────────────────────────────────────
    // Yardımcılar
    // ────────────────────────────────────────────
    private Set<String> onlySelected(Map<String, Boolean> map) {
        Set<String> out = new LinkedHashSet<>();
        for (var e : map.entrySet()) {
            if (Boolean.TRUE.equals(e.getValue()) && e.getKey() != null && !e.getKey().isBlank()) {
                out.add(e.getKey().trim());
            }
        }
        return out;
    }

    private int countMatches(Set<String> selectedHamIsimler, List<String> grupOzellikleri) {
        int count = 0;
        for (String ozellik : grupOzellikleri) {
            if (selectedHamIsimler.contains(ozellik))
                count++;
        }
        return count;
    }

    /** "3+1" → 4.0 / "5+2" → 7.0 / null/boş → null. */
    private Double parseOdaSayisi(String raw) {
        if (raw == null || raw.isBlank())
            return null;
        double total = 0;
        boolean valid = false;
        for (String part : raw.split("\\+")) {
            try {
                total += Double.parseDouble(part.trim());
                valid = true;
            } catch (NumberFormatException ignored) {
            }
        }
        return valid ? total : null;
    }

    /** scraper'daki isitma_sinif() ile aynı mantık. */
    private String isitmaAnaSinif(String raw) {
        if (raw == null || raw.isBlank())
            return UNKNOWN;
        String low = raw.toLowerCase();
        if (low.contains("yerden"))
            return "Yerden Isitma";
        if (low.contains("merkezi"))
            return "Merkezi";
        if (low.contains("kombi")) {
            if (low.contains("doğalgaz") || low.contains("dogalgaz"))
                return "Kombi Dogalgaz";
            if (low.contains("elektrik"))
                return "Kombi Elektrik";
            return "Kombi";
        }
        if (low.contains("kat kaloriferi"))
            return "Kat Kaloriferi";
        if (low.contains("vrv"))
            return "VRV";
        if (low.contains("fancoil"))
            return "Fancoil";
        if (low.contains("ısı pompası") || low.contains("isi pompasi"))
            return "Isi Pompasi";
        if (low.contains("jeotermal"))
            return "Jeotermal";
        if (low.contains("doğalgaz sobası") || low.contains("dogalgaz sobasi"))
            return "Dogalgaz Sobasi";
        if (low.contains("soba"))
            return "Soba";
        if (low.contains("klima"))
            return "Klima";
        if (low.contains("şömine") || low.contains("somine"))
            return "Somine";
        if (low.contains("güneş") || low.contains("gunes"))
            return "Gunes Enerjisi";
        if (low.equals("yok"))
            return "Yok";
        return raw;
    }

    private String mutfakAcikYokVar(String mutfakRaw) {
        if (mutfakRaw == null)
            return "yok";
        return mutfakRaw.toLowerCase().contains("açık") ? "var" : "yok";
    }

    /** Python'daki clean_attr_name ile birebir aynı sonucu üretir. */
    public static String cleanAttrName(String name) {
        if (name == null)
            return "";
        String s = name
                .replace("ç", "c").replace("Ç", "C")
                .replace("ğ", "g").replace("Ğ", "G")
                .replace("ı", "i").replace("İ", "I")
                .replace("ö", "o").replace("Ö", "O")
                .replace("ş", "s").replace("Ş", "S")
                .replace("ü", "u").replace("Ü", "U")
                .replace("&", " ve ");
        s = s.replaceAll("[()]", "")
                .replaceAll("[/\\-]", "_")
                .replaceAll("\\s+", "_")
                .replaceAll("_+", "_")
                .trim();
        return s;
    }

    /** Python'daki clean_nominal_value ile aynı çıktı. */
    private String cleanNominal(String val) {
        if (val == null || val.isBlank())
            return UNKNOWN;
        String lower = val.toLowerCase().trim();
        if (lower.equals("bilinmiyor") || lower.equals("nan") || lower.equals("none")
                || lower.equals("null") || lower.equals("-") || lower.equals("?")) {
            return UNKNOWN;
        }
        String s = val
                .replace("ç", "c").replace("Ç", "C")
                .replace("ğ", "g").replace("Ğ", "G")
                .replace("ı", "i").replace("İ", "I")
                .replace("ö", "o").replace("Ö", "O")
                .replace("ş", "s").replace("Ş", "S")
                .replace("ü", "u").replace("Ü", "U")
                .replace("&", " ve ");
        s = s.replaceAll("[()',]", "")
                .replaceAll("[/\\-]", "_")
                .replaceAll("\\s+", "_")
                .replaceAll("_+", "_")
                .trim();
        return s.isEmpty() ? UNKNOWN : s;
    }

    private String boolToYokVar(Boolean value) {
        return Boolean.TRUE.equals(value) ? "var" : "yok";
    }

    private String safeString(String value) {
        return value == null ? "" : value.trim();
    }
}