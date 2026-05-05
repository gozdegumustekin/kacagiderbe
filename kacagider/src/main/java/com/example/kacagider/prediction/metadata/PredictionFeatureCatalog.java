package com.example.kacagider.prediction.metadata;

import com.example.kacagider.prediction.dto.FeatureItemDto;
import com.example.kacagider.prediction.dto.FormFieldDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Frontend'e form metadata sağlar ve UI listesi (FeatureCatalogConstants)
 * ile pipeline config (FeaturePipelineConfig) arasında köprü kurar.
 *
 * <p>
 * Frontend'in gördüğü tüm özellik seti SABİTTİR (FeatureCatalogConstants).
 * Pipeline config sadece "modele nasıl gönderilecek?" sorusunu cevaplar.
 */
@Component
public class PredictionFeatureCatalog {

        private final FeaturePipelineConfig config;

        @Autowired
        public PredictionFeatureCatalog(FeaturePipelineConfig config) {
                this.config = config;
        }

        // ─────────────────────────────────────────────
        // STATİK FORM ALANLARI
        // ─────────────────────────────────────────────
        public static final List<FormFieldDto> TEMEL_ALANLAR = List.of(
                        new FormFieldDto("il", "İl", "select"),
                        new FormFieldDto("ilce", "İlçe", "select"),
                        new FormFieldDto("mahalle", "Mahalle", "select"),
                        new FormFieldDto("emlakTipi", "Emlak Tipi", "select"),
                        new FormFieldDto("metrekareBrut", "Brüt Metrekare", "numeric"),
                        new FormFieldDto("metrekareNet", "Net Metrekare", "numeric"),
                        new FormFieldDto("odaSayisiRaw", "Oda Sayısı", "select"),
                        new FormFieldDto("binaYasiRaw", "Bina Yaşı", "select"),
                        new FormFieldDto("bulunduguKatRaw", "Bulunduğu Kat", "select"),
                        new FormFieldDto("katSayisi", "Binadaki Kat Sayısı", "numeric"),
                        new FormFieldDto("isitmaRaw", "Isıtma Tipi", "select"),
                        new FormFieldDto("banyoSayisi", "Banyo Sayısı", "numeric"),
                        new FormFieldDto("mutfakRaw", "Mutfak Tipi", "select"));

        public static final List<FeatureItemDto> TEMEL_BINARY_ALANLAR = List.of(
                        new FeatureItemDto("balkon", "Balkon"),
                        new FeatureItemDto("asansor", "Asansör"),
                        new FeatureItemDto("otopark", "Otopark"),
                        new FeatureItemDto("esyali", "Eşyalı"));

        // ─────────────────────────────────────────────
        // SELECT SEÇENEKLERİ
        // ─────────────────────────────────────────────
        public static final List<String> EMLAK_TIPI_SECENEKLERI = List.of("Satılık", "Kiralık");

        public static final List<String> ODA_SAYISI_SECENEKLERI = List.of(
                        "1+0", "1+1", "2+1", "3+1", "4+1", "5+1", "6+1", "7+1 ve üzeri",
                        "2+2", "3+2", "4+2", "5+2");

        public static final List<String> BINA_YASI_SECENEKLERI = List.of(
                        "0", "1-5 arası", "6-10 arası", "11-15 arası",
                        "16-20 arası", "21-25 arası", "26-30 arası", "31 ve üzeri");

        public static final List<String> BULUNDUGU_KAT_SECENEKLERI = buildKatSecenekleri();

        public static final List<String> ISITMA_SECENEKLERI = List.of(
                        "Kombi (Doğalgaz)", "Kombi (Elektrik)", "Merkezi", "Merkezi (Pay Ölçer)",
                        "Yerden Isıtma", "Kat Kaloriferi", "Doğalgaz Sobası", "Soba",
                        "Klima", "Şömine", "VRV", "Fancoil", "Isı Pompası",
                        "Jeotermal", "Güneş Enerjisi", "Yok");

        public static final List<String> MUTFAK_SECENEKLERI = List.of("Açık", "Kapalı");

        private static List<String> buildKatSecenekleri() {
                List<String> list = new ArrayList<>();
                list.add("Bodrum");
                list.add("Zemin");
                list.add("Giriş");
                list.add("Yüksek Giriş");
                list.add("Bahçe");
                list.add("Çatı");
                for (int i = 1; i <= 50; i++)
                        list.add(String.valueOf(i));
                return List.copyOf(list);
        }

        public Map<String, List<String>> getSecenekListeleri() {
                Map<String, List<String>> map = new LinkedHashMap<>();
                map.put("emlakTipi", EMLAK_TIPI_SECENEKLERI);
                map.put("odaSayisi", ODA_SAYISI_SECENEKLERI);
                map.put("binaYasi", BINA_YASI_SECENEKLERI);
                map.put("bulunduguKat", BULUNDUGU_KAT_SECENEKLERI);
                map.put("isitma", ISITMA_SECENEKLERI);
                map.put("mutfak", MUTFAK_SECENEKLERI);
                return Map.copyOf(map);
        }

        // ─────────────────────────────────────────────
        // UI GRUPLARI — frontend'in göstereceği tam liste (155 özellik)
        // Her FeatureItemDto.key = ham CSV ismi (ör. "Hamam", "Yüzme Havuzu (Açık)")
        // Frontend bu key'leri Map<String,Boolean> kullanırken aynen geri yollar.
        // ─────────────────────────────────────────────
        public Map<String, List<FeatureItemDto>> getUiGruplari() {
                return FeatureCatalogConstants.UI_GRUPLARI;
        }

        public Set<String> getTumOzellikIsimleri() {
                return FeatureCatalogConstants.TUM_OZELLIK_ISIMLERI;
        }

        // ─────────────────────────────────────────────
        // PIPELINE BİLGİSİ — frontend bilgilendirme amacıyla görebilir
        // (örneğin "Hamam → luks skoruna katkı sağlar" gibi etiketler basabilir)
        // ─────────────────────────────────────────────
        /**
         * Her UI grubundaki her özelliğin pipeline'daki rolü:
         * "bireysel" = ayrı binary feature olarak modele gider
         * "skor:luks" = luks skoruna katkı sağlar (count'a girer)
         * "kullanilmiyor" = config'de yok, model bilinçli olarak görmez
         */
        public Map<String, String> getPipelineRolleri() {
                LinkedHashMap<String, String> roller = new LinkedHashMap<>();
                for (List<FeatureItemDto> grup : FeatureCatalogConstants.UI_GRUPLARI.values()) {
                        for (FeatureItemDto item : grup) {
                                String hamIsim = item.key();
                                if (config.bireyseldeMi(hamIsim)) {
                                        roller.put(hamIsim, "bireysel");
                                } else {
                                        String grupAdi = config.skorGrubunuBul(hamIsim);
                                        roller.put(hamIsim, grupAdi != null ? "skor:" + grupAdi : "kullanilmiyor");
                                }
                        }
                }
                return Map.copyOf(roller);
        }

        // ─────────────────────────────────────────────
        // YÖNLER — sabit
        // ─────────────────────────────────────────────
        public static final List<FeatureItemDto> YONLER = List.of(
                        new FeatureItemDto("Kuzey", "Kuzey"),
                        new FeatureItemDto("Güney", "Güney"),
                        new FeatureItemDto("Doğu", "Doğu"),
                        new FeatureItemDto("Batı", "Batı"));

        public List<FeatureItemDto> getYonler() {
                return YONLER;
        }
}