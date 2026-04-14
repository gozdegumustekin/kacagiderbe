package com.example.kacagider.prediction.metadata;

import com.example.kacagider.prediction.dto.FeatureItemDto;
import com.example.kacagider.prediction.dto.FormFieldDto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PredictionFeatureCatalog {

    private PredictionFeatureCatalog() {
    }

    public static final List<FormFieldDto> TEMEL_ALANLAR = List.of(
            new FormFieldDto("il", "İl", "select"),
            new FormFieldDto("ilce", "İlçe", "select"),
            new FormFieldDto("mahalle", "Mahalle", "select"),
            new FormFieldDto("emlakTipi", "Emlak Tipi (Satılık/Kiralık)", "select"),
            new FormFieldDto("metrekareBrut", "Brüt Metrekare", "numeric"),
            new FormFieldDto("metrekareNet", "Net Metrekare", "numeric"),
            new FormFieldDto("odaSayisiRaw", "Oda Sayısı (Örn: 3+1)", "select"),
            new FormFieldDto("binaYasiRaw", "Bina Yaşı", "select"),
            new FormFieldDto("bulunduguKatRaw", "Bulunduğu Kat", "select"),
            new FormFieldDto("katSayisi", "Binadaki Kat Sayısı", "numeric"),
            new FormFieldDto("isitmaRaw", "Isıtma Tipi", "select"),
            new FormFieldDto("banyoSayisi", "Banyo Sayısı", "numeric"),
            new FormFieldDto("mutfakRaw", "Mutfak Tipi", "select"));

    public static final List<FeatureItemDto> TEMEL_BINARY_ALANLAR = List.of(
            item("balkon", "Balkon"),
            item("asansor", "Asansör"),
            item("otopark", "Otopark"),
            item("esyali", "Eşyalı"));

    public static final List<FeatureItemDto> YONLER = List.of(
            item("Kuzey", "Kuzey"),
            item("Guney", "Güney"),
            item("Dogu", "Doğu"),
            item("Bati", "Batı"));

    public static final Map<String, List<FeatureItemDto>> SKOR_GRUPLARI = createScoreGroups();

    public static final Map<String, List<FeatureItemDto>> DIGER_OZELLIK_GRUPLARI = createOtherGroups();

    public static final Map<String, Set<String>> SKOR_KEY_MAP = createScoreKeyMap();

    public static final Set<String> TUM_SECILEBILIR_OZELLIK_KEYLERI = createAllSelectableKeys();

    private static Map<String, List<FeatureItemDto>> createScoreGroups() {
        LinkedHashMap<String, List<FeatureItemDto>> map = new LinkedHashMap<>();

        map.put("guvenlik", List.of(
                item("Alarm_Hirsiz", "Alarm (Hırsız)"),
                item("Alarm_Yangin", "Alarm (Yangın)"),
                item("24_Saat_Guvenlik", "24 Saat Güvenlik"),
                item("Goruntulu_Diyafon", "Görüntülü Diyafon"),
                item("Kamera_Sistemi", "Kamera Sistemi"),
                item("Yuz_Tanima_ve_Parmak_Izi", "Yüz Tanıma & Parmak İzi"),
                item("Yangin_Merdiveni", "Yangın Merdiveni")));

        map.put("luks_konfor", List.of(
                item("Akilli_Ev", "Akıllı Ev"),
                item("Somine", "Şömine"),
                item("Hamam", "Hamam"),
                item("Sauna", "Sauna"),
                item("Jakuzi", "Jakuzi"),
                item("Yuzme_Havuzu_Acik", "Yüzme Havuzu (Açık)"),
                item("Yuzme_Havuzu_Kapali", "Yüzme Havuzu (Kapalı)"),
                item("Yuzme_Havuzu", "Yüzme Havuzu"),
                item("Mustakil_Havuzlu", "Müstakil Havuzlu"),
                item("Ebeveyn_Banyosu", "Ebeveyn Banyosu"),
                item("Giyinme_Odasi", "Giyinme Odası"),
                item("Teras", "Teras"),
                item("Arac_Sarj_Istasyonu", "Araç Şarj İstasyonu"),
                item("Buhar_Odasi", "Buhar Odası")));

        map.put("sosyal_tesis", List.of(
                item("Cocuk_Oyun_Parki", "Çocuk Oyun Parkı"),
                item("Kres", "Kreş"),
                item("Spor_Alani", "Spor Alanı"),
                item("Spor_Salonu", "Spor Salonu"),
                item("Tenis_Kortu", "Tenis Kortu"),
                item("Kopek_Parki", "Köpek Parkı"),
                item("Alisveris_Merkezi", "Alışveriş Merkezi"),
                item("Eglence_Merkezi", "Eğlence Merkezi"),
                item("Park", "Park"),
                item("Park_ve_Yesil_Alan", "Park & Yeşil Alan")));

        map.put("lokasyon_ulasim", List.of(
                item("Metro", "Metro"),
                item("Metrobus", "Metrobüs"),
                item("Marmaray", "Marmaray"),
                item("Avrasya_Tuneli", "Avrasya Tüneli"),
                item("Bogaz_Kopruleri", "Boğaz Köprüleri"),
                item("E_5", "E-5"),
                item("TEM", "TEM"),
                item("Deniz_Otobusu", "Deniz Otobüsü"),
                item("Havaalani", "Havaalanı"),
                item("Iskele", "İskele"),
                item("Tramvay", "Tramvay"),
                item("Tren_Istasyonu", "Tren İstasyonu"),
                item("Denize_Sifir", "Denize Sıfır"),
                item("Gole_Sifir", "Göle Sıfır"),
                item("Bogaz", "Boğaz"),
                item("Deniz", "Deniz"),
                item("Sahil", "Sahil")));

        return Map.copyOf(map);
    }

    private static Map<String, List<FeatureItemDto>> createOtherGroups() {
        LinkedHashMap<String, List<FeatureItemDto>> map = new LinkedHashMap<>();

        map.put("ic_ozellikler", List.of(
                item("ADSL", "ADSL"),
                item("Ahsap_Dograma", "Ahşap Doğrama"),
                item("Alaturka_Tuvalet", "Alaturka Tuvalet"),
                item("Aluminyum_Dograma", "Alüminyum Doğrama"),
                item("Amerikan_Kapi", "Amerikan Kapı"),
                item("Ankastre_Firin", "Ankastre Fırın"),
                item("Barbeku", "Barbekü"),
                item("Beyaz_Esya", "Beyaz Eşya"),
                item("Boyali", "Boyalı"),
                item("Bulasik_Makinesi", "Bulaşık Makinesi"),
                item("Buzdolabi", "Buzdolabı"),
                item("Camasir_Kurutma_Makinesi", "Çamaşır Kurutma Makinesi"),
                item("Camasir_Makinesi", "Çamaşır Makinesi"),
                item("Camasir_Odasi", "Çamaşır Odası"),
                item("Celik_Kapi", "Çelik Kapı"),
                item("Dusakabin", "Duşakabin"),
                item("Duvar_Kagidi", "Duvar Kağıdı"),
                item("Firin", "Fırın"),
                item("Fiber_Internet", "Fiber İnternet"),
                item("Gomme_Dolap", "Gömme Dolap"),
                item("Hilton_Banyo", "Hilton Banyo"),
                item("Intercom_Sistemi", "Intercom Sistemi"),
                item("Isicam", "Isıcam"),
                item("Kartonpiyer", "Kartonpiyer"),
                item("Kiler", "Kiler"),
                item("Klima", "Klima"),
                item("Kuvet", "Küvet"),
                item("Laminat_Zemin", "Laminat Zemin"),
                item("Marley", "Marley"),
                item("Mobilya", "Mobilya"),
                item("Mutfak_Ankastre", "Mutfak (Ankastre)"),
                item("Mutfak_Laminat", "Mutfak (Laminat)"),
                item("Mutfak_Dogalgazi", "Mutfak Doğalgazı"),
                item("Panjur_Jaluzi", "Panjur/Jaluzi"),
                item("Parke_Zemin", "Parke Zemin"),
                item("PVC_Dograma", "PVC Doğrama"),
                item("Seramik_Zemin", "Seramik Zemin"),
                item("Set_Ustu_Ocak", "Set Üstü Ocak"),
                item("Spot_Aydinlatma", "Spot Aydınlatma"),
                item("Sofben", "Şofben"),
                item("Termosifon", "Termosifon"),
                item("Vestiyer", "Vestiyer")));

        map.put("bina_site", List.of(
                item("Apartman_Gorevlisi", "Apartman Görevlisi"),
                item("Hidrofor", "Hidrofor"),
                item("Isi_Yalitimi", "Isı Yalıtımı"),
                item("Jenerator", "Jeneratör"),
                item("Kablo_TV", "Kablo TV"),
                item("Ses_Yalitimi", "Ses Yalıtımı"),
                item("Siding", "Siding"),
                item("Su_Deposu", "Su Deposu"),
                item("Uydu", "Uydu"),
                item("Arac_Park_Yeri", "Araç Park Yeri")));

        map.put("cevre_sosyal_yakinlik", List.of(
                item("Belediye", "Belediye"),
                item("Cami", "Cami"),
                item("Cemevi", "Cemevi"),
                item("Eczane", "Eczane"),
                item("Fuar", "Fuar"),
                item("Hastane", "Hastane"),
                item("Havra", "Havra"),
                item("Ilkokul_Ortaokul", "İlkokul-Ortaokul"),
                item("Itfaiye", "İtfaiye"),
                item("Kilise", "Kilise"),
                item("Lise", "Lise"),
                item("Market", "Market"),
                item("Plaj", "Plaj"),
                item("Polis_Merkezi", "Polis Merkezi"),
                item("Saglik_Ocagi", "Sağlık Ocağı"),
                item("Semt_Pazari", "Semt Pazarı"),
                item("Sehir_Merkezi", "Şehir Merkezi"),
                item("Universite", "Üniversite")));

        map.put("ulasim_diger", List.of(
                item("Anayol", "Anayol"),
                item("Cadde", "Cadde"),
                item("Dolmus", "Dolmuş"),
                item("Minibus", "Minibüs"),
                item("Otobus_Duragi", "Otobüs Durağı")));

        map.put("manzara", List.of(
                item("Doga", "Doğa"),
                item("Gol", "Göl"),
                item("Havuz", "Havuz"),
                item("Nehir", "Nehir"),
                item("Sehir", "Şehir")));

        map.put("konut_tipi_detaylari", List.of(
                item("Dubleks", "Dubleks"),
                item("En_Ust_Kat", "En Üst Kat"),
                item("Ara_Kat", "Ara Kat"),
                item("Ara_Kat_Dubleks", "Ara Kat Dubleks"),
                item("Bahce_Dubleksi", "Bahçe Dubleksi"),
                item("Cati_Dubleksi", "Çatı Dubleksi"),
                item("Forleks", "Forleks"),
                item("Ters_Dubleks", "Ters Dubleks"),
                item("Tripleks", "Tripleks")));

        map.put("engelli_uygunluk", List.of(
                item("Engelliye_Uygun_Asansor", "Engelliye Uygun Asansör"),
                item("Engelliye_Uygun_Banyo", "Engelliye Uygun Banyo"),
                item("Engelliye_Uygun_Mutfak", "Engelliye Uygun Mutfak"),
                item("Engelliye_Uygun_Park", "Engelliye Uygun Park"),
                item("Genis_Koridor", "Geniş Koridor"),
                item("Giris_Rampa", "Giriş / Rampa"),
                item("Merdiven", "Merdiven"),
                item("Oda_Kapisi", "Oda Kapısı"),
                item("Priz_Elektrik_Anahtari", "Priz / Elektrik Anahtarı"),
                item("Tutamak_Korkuluk", "Tutamak / Korkuluk"),
                item("Tuvalet", "Tuvalet")));

        return Map.copyOf(map);
    }

    private static Map<String, Set<String>> createScoreKeyMap() {
        LinkedHashMap<String, Set<String>> map = new LinkedHashMap<>();
        for (Map.Entry<String, List<FeatureItemDto>> entry : SKOR_GRUPLARI.entrySet()) {
            Set<String> keys = new LinkedHashSet<>();
            for (FeatureItemDto item : entry.getValue()) {
                keys.add(item.key());
            }
            map.put(entry.getKey(), Set.copyOf(keys));
        }
        return Map.copyOf(map);
    }

    private static Set<String> createAllSelectableKeys() {
        Set<String> all = new LinkedHashSet<>();

        for (List<FeatureItemDto> group : SKOR_GRUPLARI.values()) {
            for (FeatureItemDto item : group) {
                all.add(item.key());
            }
        }

        for (List<FeatureItemDto> group : DIGER_OZELLIK_GRUPLARI.values()) {
            for (FeatureItemDto item : group) {
                all.add(item.key());
            }
        }

        return Set.copyOf(all);
    }

    public static Set<String> getScoreKeys(String groupKey) {
        return SKOR_KEY_MAP.getOrDefault(groupKey, Set.of());
    }

    private static FeatureItemDto item(String key, String label) {
        return new FeatureItemDto(key, label);
    }
}