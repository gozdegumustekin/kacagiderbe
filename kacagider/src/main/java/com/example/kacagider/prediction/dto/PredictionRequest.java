package com.example.kacagider.prediction.dto;

import java.util.List;

public record PredictionRequest(
        String il,
        String ilce,
        String mahalle,
        String emlakTipi,
        Double metrekareBrut,
        Double metrekareNet,
        String odaSayisiRaw,
        String binaYasiRaw,
        String bulunduguKatRaw,
        Integer katSayisi,
        String isitmaRaw,
        Integer banyoSayisi,
        String mutfakRaw,

        Boolean balkon,
        Boolean asansor,
        Boolean otopark,
        Boolean esyali,

        List<String> yonler,
        List<String> seciliOzellikler) {
}