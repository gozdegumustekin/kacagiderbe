package com.example.kacagider.prediction.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmlakPreprocessor {

    public static Map<String, Object> process(Map<String, Object> rawFeatures) {
        Map<String, Object> processed = new HashMap<>(rawFeatures);

        // 1. Oda Sayısı Hesaplama
        String odaRaw = String.valueOf(rawFeatures.getOrDefault("oda_sayisi_raw", "1+1"));
        processed.put("oda_sayisi", calculateOdaSayisi(odaRaw));

        // 2. Bina Yaşı (Sayısal ve Ordinal)
        String yasRaw = String.valueOf(rawFeatures.getOrDefault("bina_yasi_raw", "0"));
        processed.put("bina_yasi_numeric", calculateYasNumeric(yasRaw));
        processed.put("bina_yasi_ordinal", calculateYasOrdinal(yasRaw));

        // 3. Bulunduğu Kat (Sayısal ve Ordinal)
        String katRaw = String.valueOf(rawFeatures.getOrDefault("bulundugu_kat_raw", "0"));
        processed.put("bulundugu_kat_no", calculateKatNo(katRaw));
        processed.put("bulundugu_kat_ordinal", calculateKatOrdinal(katRaw));

        // 4. Isıtma Puanı (Score)
        String isitmaRaw = String.valueOf(rawFeatures.getOrDefault("isitma_raw", "Yok"));
        processed.put("isitma_score", calculateIsitmaScore(isitmaRaw));

        // 5. Mutfak Açık mı?
        String mutfakRaw = String.valueOf(rawFeatures.getOrDefault("mutfak_raw", ""));
        processed.put("mutfak_acik_mi", mutfakRaw.toLowerCase().contains("açık") ? 1.0 : 0.0);

        // 6. Boolean Çevrimleri (Örn: "var" -> 1.0)
        String[] boolFields = { "balkon", "asansor", "otopark", "esyali" };
        for (String field : boolFields) {
            String val = String.valueOf(rawFeatures.getOrDefault(field, "yok")).toLowerCase();
            processed.put(field, val.equals("var") ? 1.0 : 0.0);
        }

        return processed;
    }

    private static double extractNumber(String text) {
        if (text == null)
            return 0.0;
        Matcher m = Pattern.compile("-?\\d+").matcher(text);
        if (m.find()) {
            return Double.parseDouble(m.group());
        }
        return 0.0;
    }

    private static double calculateOdaSayisi(String metin) {
        if (metin == null)
            return 0.0;
        Matcher m = Pattern.compile("\\d+").matcher(metin);
        double sum = 0;
        while (m.find())
            sum += Double.parseDouble(m.group());
        return sum > 0 ? sum : 1.0;
    }

    private static double calculateYasOrdinal(String metin) {
        if (metin == null)
            return 0.0;
        metin = metin.toLowerCase().trim();

        if (metin.contains("sıfır") || metin.equals("0"))
            return 5.0;
        if (metin.contains("0-5"))
            return 4.0;
        if (metin.contains("6-10"))
            return 3.0;
        if (metin.contains("11-15") || metin.contains("11-25"))
            return 2.0;
        if (metin.contains("26") || metin.contains("üzeri"))
            return 1.0;

        double sayi = extractNumber(metin);
        if (sayi == 0)
            return 0.0;
        if (sayi <= 5)
            return 4.0;
        if (sayi <= 10)
            return 3.0;
        if (sayi <= 25)
            return 2.0;
        return 1.0;
    }

    private static double calculateYasNumeric(String metin) {
        if (metin == null)
            return 0.0;
        metin = metin.toLowerCase().trim();

        if (metin.contains("sıfır") || metin.equals("0"))
            return 0.0;
        if (metin.contains("0-5"))
            return 3.0;
        if (metin.contains("6-10"))
            return 8.0;
        if (metin.contains("11-15"))
            return 13.0;
        if (metin.contains("11-25"))
            return 18.0;
        if (metin.contains("26") || metin.contains("üzeri"))
            return 30.0;

        return extractNumber(metin);
    }

    private static double calculateKatOrdinal(String metin) {
        if (metin == null)
            return 0.0;
        metin = metin.toLowerCase().trim();

        if (metin.contains("bodrum") || metin.contains("kot") || metin.contains("giriş altı"))
            return 1.0;
        if (metin.contains("çatı"))
            return 2.0;
        if (metin.contains("bahçe") || metin.contains("yüksek giriş"))
            return 4.0;
        if (metin.contains("müstakil") || metin.contains("villa"))
            return 5.0;
        if (metin.contains("giriş") || metin.contains("zemin"))
            return 3.0;

        double katNo = extractNumber(metin);
        if (katNo >= 1 && katNo <= 5)
            return 5.0;
        if (katNo >= 6 && katNo <= 10)
            return 4.0;
        if (katNo >= 11 && katNo <= 20)
            return 3.0;
        if (katNo >= 21)
            return 2.0;

        return 0.0;
    }

    private static double calculateKatNo(String metin) {
        if (metin == null)
            return 0.0;
        metin = metin.toLowerCase().trim();

        if (metin.contains("bodrum"))
            return -1.0;
        if (metin.contains("giriş altı") || metin.contains("kot"))
            return -0.5;
        if (metin.contains("bahçe") || metin.contains("zemin") || metin.contains("giriş"))
            return 0.0;
        if (metin.contains("yüksek giriş"))
            return 0.5;
        if (metin.contains("çatı"))
            return 99.0;
        if (metin.contains("müstakil") || metin.contains("villa"))
            return 1.0;

        return extractNumber(metin);
    }

    private static double calculateIsitmaScore(String metin) {
        if (metin == null)
            return 0.0;
        metin = metin.toLowerCase().trim();
        double tabanPuan = 0.0;

        if (metin.contains("kombi (doğalgaz)") || metin.contains("yerden") ||
                metin.contains("pay ölçer") || metin.contains("ısı pompası") || metin.contains("jeotermal")) {
            tabanPuan = 5.0;
        } else if (metin.contains("merkezi") || metin.contains("kat kaloriferi") || metin.contains("vrv")) {
            tabanPuan = 4.0;
        } else if (metin.contains("kombi (elektrik)") || metin.contains("fancoil")) {
            tabanPuan = 3.0;
        } else if (metin.contains("doğalgaz sobası") || metin.contains("elektrikli radyatör") ||
                metin.contains("şömine") || metin.contains("güneş enerjisi")) {
            tabanPuan = 2.0;
        } else if (metin.equals("yok") || metin.contains("soba")) {
            tabanPuan = 1.0;
        }

        if (metin.contains("klima")) {
            if (tabanPuan >= 4.0)
                return tabanPuan + 0.5;
            else
                tabanPuan = Math.max(tabanPuan, 3.0);
        }

        return tabanPuan;
    }
}