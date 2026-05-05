package com.example.kacagider.prediction.metadata;

import com.example.kacagider.prediction.dto.FeatureItemDto;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * TÜM 155 özelliğin sabit listesi — frontend'in form'da göstereceği tam katalog.
 *
 * <p><b>Bu sınıfın pipeline ile hiçbir bağlantısı yoktur.</b> Pipeline değiştiğinde
 * (örneğin "Hamam" bireyselden skor'a alındığında) bu liste değişmez. Frontend her
 * zaman tüm özellikleri görür; pipeline kararı runtime'da arka planda uygulanır.
 *
 * <p>Buradaki "ham isim" (Türkçe karakterli, scraper'ın CSV'ye yazdığı isim)
 * pipeline_config.json'daki isimlerle birebir eşleşmek ZORUNDADIR.
 *
 * <p>UI gruplaması (manzara, lüks, güvenlik...) frontend'in renderdaki başlıkları
 * için. Pipeline'ın "skor grubu" gruplamasıyla karıştırma — bu sadece görsel.
 */
public final class FeatureCatalogConstants {

    private FeatureCatalogConstants() {}

    /**
     * UI'da gösterilecek tüm özellikler, anlamlı başlıklar altında.
     * key = ham CSV ismi (ör. "Hamam", "Yüzme Havuzu (Açık)"),
     * label = Türkçe etiket (genelde aynı), backend bunu clean_attr_name ile
     * Java tarafında temizleyip karşılaştırır.
     */
    public static final Map<String, List<FeatureItemDto>> UI_GRUPLARI = createUiGruplari();

    /** Tüm ham özellik isimlerinin düz listesi (155 adet). */
    public static final Set<String> TUM_OZELLIK_ISIMLERI = createTumIsimler();

    private static Map<String, List<FeatureItemDto>> createUiGruplari() {
        LinkedHashMap<String, List<FeatureItemDto>> map = new LinkedHashMap<>();

        // Yön (4)
        map.put("yon", List.of(
                item("Kuzey", "Kuzey"), item("Güney", "Güney"),
                item("Doğu", "Doğu"), item("Batı", "Batı")));

        // Manzara (8)
        map.put("manzara", List.of(
                item("Deniz", "Deniz"), item("Boğaz", "Boğaz"),
                item("Göl", "Göl"), item("Nehir", "Nehir"),
                item("Şehir", "Şehir"), item("Doğa", "Doğa"),
                item("Park & Yeşil Alan", "Park & Yeşil Alan"),
                item("Havuz", "Havuz")));

        // Konut tipi detayları (9)
        map.put("konut_tipi_detaylari", List.of(
                item("Dubleks", "Dubleks"), item("Tripleks", "Tripleks"),
                item("Çatı Dubleksi", "Çatı Dubleksi"),
                item("Bahçe Dubleksi", "Bahçe Dubleksi"),
                item("Forleks", "Forleks"), item("Ters Dubleks", "Ters Dubleks"),
                item("Ara Kat", "Ara Kat"), item("Ara Kat Dubleks", "Ara Kat Dubleks"),
                item("En Üst Kat", "En Üst Kat")));

        // Güvenlik (8)
        map.put("guvenlik", List.of(
                item("Alarm (Hırsız)", "Alarm (Hırsız)"),
                item("Alarm (Yangın)", "Alarm (Yangın)"),
                item("24 Saat Güvenlik", "24 Saat Güvenlik"),
                item("Görüntülü Diyafon", "Görüntülü Diyafon"),
                item("Kamera Sistemi", "Kamera Sistemi"),
                item("Intercom Sistemi", "Intercom Sistemi"),
                item("Yüz Tanıma & Parmak İzi", "Yüz Tanıma & Parmak İzi"),
                item("Yangın Merdiveni", "Yangın Merdiveni")));

        // Lüks & konfor (15)
        map.put("luks_konfor", List.of(
                item("Akıllı Ev", "Akıllı Ev"), item("Şömine", "Şömine"),
                item("Hamam", "Hamam"), item("Sauna", "Sauna"), item("Jakuzi", "Jakuzi"),
                item("Buhar Odası", "Buhar Odası"),
                item("Yüzme Havuzu (Açık)", "Yüzme Havuzu (Açık)"),
                item("Yüzme Havuzu (Kapalı)", "Yüzme Havuzu (Kapalı)"),
                item("Yüzme Havuzu", "Yüzme Havuzu"),
                item("Müstakil Havuzlu", "Müstakil Havuzlu"),
                item("Ebeveyn Banyosu", "Ebeveyn Banyosu"),
                item("Giyinme Odası", "Giyinme Odası"),
                item("Hilton Banyo", "Hilton Banyo"),
                item("Teras", "Teras"),
                item("Araç Şarj İstasyonu", "Araç Şarj İstasyonu")));

        // Site içi sosyal (10)
        map.put("sosyal_tesis", List.of(
                item("Çocuk Oyun Parkı", "Çocuk Oyun Parkı"),
                item("Kreş", "Kreş"),
                item("Spor Alanı", "Spor Alanı"),
                item("Spor Salonu", "Spor Salonu"),
                item("Tenis Kortu", "Tenis Kortu"),
                item("Köpek Parkı", "Köpek Parkı"),
                item("Park", "Park"),
                item("Barbekü", "Barbekü"),
                item("Alışveriş Merkezi", "Alışveriş Merkezi"),
                item("Eğlence Merkezi", "Eğlence Merkezi")));

        // Ulaşım (20)
        map.put("ulasim", List.of(
                item("Metro", "Metro"), item("Metrobüs", "Metrobüs"),
                item("Marmaray", "Marmaray"), item("Tramvay", "Tramvay"),
                item("Tren İstasyonu", "Tren İstasyonu"),
                item("Avrasya Tüneli", "Avrasya Tüneli"),
                item("Boğaz Köprüleri", "Boğaz Köprüleri"),
                item("E-5", "E-5"), item("TEM", "TEM"),
                item("Cadde", "Cadde"), item("Anayol", "Anayol"),
                item("Otobüs Durağı", "Otobüs Durağı"),
                item("Minibüs", "Minibüs"), item("Dolmuş", "Dolmuş"),
                item("Deniz Otobüsü", "Deniz Otobüsü"),
                item("Havaalanı", "Havaalanı"), item("İskele", "İskele"),
                item("Denize Sıfır", "Denize Sıfır"),
                item("Göle Sıfır", "Göle Sıfır"),
                item("Sahil", "Sahil")));

        // Çevre / yakınlık (20)
        map.put("cevre_yakinlik", List.of(
                item("Hastane", "Hastane"), item("Eczane", "Eczane"),
                item("Sağlık Ocağı", "Sağlık Ocağı"),
                item("Cami", "Cami"), item("Cemevi", "Cemevi"),
                item("Havra", "Havra"), item("Kilise", "Kilise"),
                item("İlkokul-Ortaokul", "İlkokul-Ortaokul"),
                item("Lise", "Lise"), item("Üniversite", "Üniversite"),
                item("Market", "Market"),
                item("Şehir Merkezi", "Şehir Merkezi"),
                item("Belediye", "Belediye"),
                item("Polis Merkezi", "Polis Merkezi"),
                item("İtfaiye", "İtfaiye"),
                item("Semt Pazarı", "Semt Pazarı"),
                item("Fuar", "Fuar"), item("Plaj", "Plaj")));

        // İç özellikler (44)
        map.put("ic_ozellikler", List.of(
                item("ADSL", "ADSL"),
                item("Ahşap Doğrama", "Ahşap Doğrama"),
                item("Alaturka Tuvalet", "Alaturka Tuvalet"),
                item("Alüminyum Doğrama", "Alüminyum Doğrama"),
                item("Amerikan Kapı", "Amerikan Kapı"),
                item("Ankastre Fırın", "Ankastre Fırın"),
                item("Beyaz Eşya", "Beyaz Eşya"),
                item("Boyalı", "Boyalı"),
                item("Bulaşık Makinesi", "Bulaşık Makinesi"),
                item("Buzdolabı", "Buzdolabı"),
                item("Çamaşır Kurutma Makinesi", "Çamaşır Kurutma Makinesi"),
                item("Çamaşır Makinesi", "Çamaşır Makinesi"),
                item("Çamaşır Odası", "Çamaşır Odası"),
                item("Duşakabin", "Duşakabin"),
                item("Duvar Kağıdı", "Duvar Kağıdı"),
                item("Fırın", "Fırın"),
                item("Gömme Dolap", "Gömme Dolap"),
                item("Kartonpiyer", "Kartonpiyer"),
                item("Kiler", "Kiler"),
                item("Klima", "Klima"),
                item("Küvet", "Küvet"),
                item("Laminat Zemin", "Laminat Zemin"),
                item("Marley", "Marley"),
                item("Mobilya", "Mobilya"),
                item("Mutfak (Ankastre)", "Mutfak (Ankastre)"),
                item("Mutfak (Laminat)", "Mutfak (Laminat)"),
                item("Mutfak Doğalgazı", "Mutfak Doğalgazı"),
                item("Panjur/Jaluzi", "Panjur/Jaluzi"),
                item("Parke Zemin", "Parke Zemin"),
                item("PVC Doğrama", "PVC Doğrama"),
                item("Seramik Zemin", "Seramik Zemin"),
                item("Set Üstü Ocak", "Set Üstü Ocak"),
                item("Spot Aydınlatma", "Spot Aydınlatma"),
                item("Şofben", "Şofben"),
                item("Termosifon", "Termosifon"),
                item("Vestiyer", "Vestiyer")));

        // Bina / site (10)
        map.put("bina_site", List.of(
                item("Apartman Görevlisi", "Apartman Görevlisi"),
                item("Hidrofor", "Hidrofor"),
                item("Isı Yalıtımı", "Isı Yalıtımı"),
                item("Jeneratör", "Jeneratör"),
                item("Kablo TV", "Kablo TV"),
                item("Ses Yalıtımı", "Ses Yalıtımı"),
                item("Siding", "Siding"),
                item("Su Deposu", "Su Deposu"),
                item("Uydu", "Uydu"),
                item("Araç Park Yeri", "Araç Park Yeri")));

        // Yüksek-sinyal teknik (5) — UI'de ayrı grup; pipeline'da bireysel olabilir
        map.put("yuksek_sinyal_teknik", List.of(
                item("Çelik Kapı", "Çelik Kapı"),
                item("Isıcam", "Isıcam"),
                item("Fiber İnternet", "Fiber İnternet")));
        // (Isı/Ses Yalıtımı zaten bina_site'da, mükerrer tutmuyoruz)

        // Engelli uygunluk (11)
        map.put("engelli_uygunluk", List.of(
                item("Engelliye Uygun Asansör", "Engelliye Uygun Asansör"),
                item("Engelliye Uygun Banyo", "Engelliye Uygun Banyo"),
                item("Engelliye Uygun Mutfak", "Engelliye Uygun Mutfak"),
                item("Engelliye Uygun Park", "Engelliye Uygun Park"),
                item("Geniş Koridor", "Geniş Koridor"),
                item("Giriş / Rampa", "Giriş / Rampa"),
                item("Merdiven", "Merdiven"),
                item("Oda Kapısı", "Oda Kapısı"),
                item("Priz / Elektrik Anahtarı", "Priz / Elektrik Anahtarı"),
                item("Tutamak / Korkuluk", "Tutamak / Korkuluk"),
                item("Tuvalet", "Tuvalet")));

        return Map.copyOf(map);
    }

    private static Set<String> createTumIsimler() {
        LinkedHashSet<String> all = new LinkedHashSet<>();
        for (List<FeatureItemDto> grup : UI_GRUPLARI.values()) {
            for (FeatureItemDto i : grup) {
                all.add(i.key());  // ham isim
            }
        }
        return Set.copyOf(all);
    }

    private static FeatureItemDto item(String hamIsim, String label) {
        return new FeatureItemDto(hamIsim, label);
    }
}
