package com.example.kacagider.prediction.dto;

import java.util.List;
import java.util.Map;

/**
 * Frontend'e gönderilen form metadata. Pipeline'dan bağımsız tam özellik
 * listesi
 * ve pipeline rollerinin haritasını içerir.
 *
 * <p>
 * Frontend bu yapıyla tüm 155 özelliği UI gruplarında render eder; istersen
 * yan tarafa "skor: luks", "bireysel" gibi rozetler basabilir.
 *
 * @param temelAlanlar       form'un üst bölümündeki temel alanlar
 * @param temelBinaryAlanlar balkon/asansör/otopark/eşyalı gibi temel binary
 *                           alanlar
 * @param yonler             4 yön (Kuzey/Güney/Doğu/Batı)
 * @param uiGruplari         TÜM 155 özellik anlamlı UI başlıkları altında —
 *                           pipeline
 *                           değişse de aynı kalır
 * @param pipelineRolleri    her özelliğin pipeline'daki rolü
 *                           ("bireysel", "skor:luks", "kullanilmiyor")
 * @param secenekListeleri   select tipi alanların seçenekleri
 *                           (emlakTipi, odaSayisi, binaYasi, bulunduguKat,
 *                           isitma, mutfak)
 * @param pipelineVersion    pipeline_config.json'daki version (debug/log için)
 */
public record FormMetadataResponse(
                List<FormFieldDto> temelAlanlar,
                List<FeatureItemDto> temelBinaryAlanlar,
                List<FeatureItemDto> yonler,
                Map<String, List<FeatureItemDto>> uiGruplari,
                Map<String, String> pipelineRolleri,
                Map<String, List<String>> secenekListeleri,
                Integer pipelineVersion) {
}