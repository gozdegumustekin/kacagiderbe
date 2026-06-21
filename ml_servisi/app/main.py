"""
app/main.py
===========
Kaça Gider — Fotoğraf Doğrulama & Kalite ML Servisi (FastAPI).

ENDPOINT'LER
------------
GET  /health
    Servis + model durumu.

WS   /ws/foto-dogrula
    Tek fotoğrafı 4 aşamadan geçirir, her aşamayı anlık bildirir,
    geçerliyse Java'ya iletir.

    İlk mesaj (client → server), JSON:
        {
          "prediction_id": "uuid",
          "jwt": "Bearer token (sadece token kısmı)",
          "beklenen_tip": "salon",
          "dosya_adi": "salon1.jpg",
          "content_type": "image/jpeg"
        }
    Ardından client BINARY frame olarak fotoğraf byte'larını gönderir.

    Server → client mesajları (JSON):
        {"tip": "asama", "asama": 1, "asama_adi": "...", "gecti": true, "mesaj": "..."}
        {"tip": "sonuc", "gecerli": true, "kalite_etiket": "Iyi", "java": {...}}
        {"tip": "hata", "mesaj": "..."}

POST /rest/foto-dogrula   (WebSocket istemeyenler için yedek)
    multipart: image, beklenen_tip, prediction_id, jwt
    Tüm aşamalar çalışır, tek JSON döner (aşama aşama bildirim yok).
"""

from __future__ import annotations

import json

from fastapi import FastAPI, WebSocket, WebSocketDisconnect, UploadFile, Form, File
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from app.core import models
from app.core.pipeline import DogrulamaHatti
from app.core.java_client import foto_ilet

app = FastAPI(title="Kaça Gider ML Servisi", version="1.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.on_event("startup")
def _startup():
    models.modelleri_yukle()

@app.get("/")
def read_root():
    return {"message": "ML Service is up and running!"}

@app.get("/health")
def health():
    yuklu = models.yuklu_modeller()
    return {
        "status": "UP",
        "service": "kacagider-ml",
        "yuklu_modeller": yuklu,
        "model_sayisi": len(yuklu),
    }


# ────────────────────────────────────────────────────────────
#  WebSocket — aşama aşama doğrulama
# ────────────────────────────────────────────────────────────
@app.websocket("/ws/foto-dogrula")
async def ws_foto_dogrula(ws: WebSocket):
    await ws.accept()
    try:
        # 1) İlk mesaj: metadata (JSON)
        meta_raw = await ws.receive_text()
        meta = json.loads(meta_raw)

        prediction_id = meta.get("prediction_id")
        jwt = meta.get("jwt", "").replace("Bearer ", "").strip()
        beklenen_tip = meta.get("beklenen_tip")
        dosya_adi = meta.get("dosya_adi", "foto.jpg")
        content_type = meta.get("content_type", "image/jpeg")

        if not prediction_id or not beklenen_tip:
            await ws.send_json({"tip": "hata",
                                "mesaj": "prediction_id ve beklenen_tip zorunlu."})
            await ws.close()
            return

        # 2) Fotoğraf byte'ları (binary frame)
        icerik = await ws.receive_bytes()

        try:
            image = models.goruntu_oku(icerik)
        except Exception as e:
            await ws.send_json({"tip": "hata", "mesaj": f"Görüntü okunamadı: {e}"})
            await ws.close()
            return

        # 3) Pipeline — her aşamada WS'e bildir
        async def asama_bildir_sync_wrapper(sonuc):
            # pipeline senkron çağırıyor; burada queue yerine doğrudan
            # gönderemiyoruz (async). Bu yüzden aşamaları toplayıp
            # calistir sonrası göndereceğiz — aşağıda hallediliyor.
            pass

        # Senkron pipeline; aşamaları biriktir, sonra hepsini sırayla yolla.
        toplanan = []
        hat = DogrulamaHatti(on_asama=lambda s: toplanan.append(s))
        sonuc = hat.calistir(image, beklenen_tip)

        # Aşamaları sırayla gönder (anlık akış hissi)
        for a in sonuc.asamalar:
            await ws.send_json({
                "tip": "asama",
                "asama": a.asama,
                "asama_adi": a.asama_adi,
                "gecti": a.gecti,
                "tahmin": a.tahmin,
                "guven": round(a.guven, 3),
                "mesaj": a.mesaj,
            })

        # 4) Geçersizse bildir ve bitir
        if not sonuc.gecerli:
            await ws.send_json({
                "tip": "sonuc",
                "gecerli": False,
                "red_asamasi": sonuc.red_asamasi,
                "mesaj": sonuc.mesaj,
            })
            await ws.close()
            return

        # 5) Geçerliyse Java'ya ilet
        java_cevap = None
        java_hata = None
        try:
            java_cevap = await foto_ilet(
                prediction_id=prediction_id,
                jwt=jwt,
                dosya_adi=dosya_adi,
                icerik=icerik,
                content_type=content_type,
                oda_tipi=sonuc.oda_tipi,
                kalite_etiket=sonuc.kalite_etiket,
                kalite_skor=sonuc.kalite_skor,
                kalite_guven=sonuc.kalite_guven,
            )
        except Exception as e:
            java_hata = str(e)

        await ws.send_json({
            "tip": "sonuc",
            "gecerli": True,
            "oda_tipi": sonuc.oda_tipi,
            "kalite_etiket": sonuc.kalite_etiket,
            "kalite_skor": sonuc.kalite_skor,
            "kalite_guven": round(sonuc.kalite_guven or 0, 3),
            "mesaj": sonuc.mesaj,
            "java_kayit": java_cevap,
            "java_hata": java_hata,
        })
        await ws.close()

    except WebSocketDisconnect:
        pass
    except Exception as e:
        try:
            await ws.send_json({"tip": "hata", "mesaj": f"Beklenmeyen hata: {e}"})
            await ws.close()
        except Exception:
            pass


# ────────────────────────────────────────────────────────────
#  REST yedek — tek seferde tüm aşamalar
# ────────────────────────────────────────────────────────────
@app.post("/rest/foto-dogrula")
async def rest_foto_dogrula(
    image: UploadFile = File(...),
    beklenen_tip: str = Form(...),
    prediction_id: str = Form(...),
    jwt: str = Form(""),
):
    icerik = await image.read()
    try:
        pil = models.goruntu_oku(icerik)
    except Exception as e:
        return JSONResponse(status_code=400, content={"hata": f"Görüntü okunamadı: {e}"})

    hat = DogrulamaHatti()
    sonuc = hat.calistir(pil, beklenen_tip)

    java_cevap = None
    java_hata = None
    if sonuc.gecerli:
        try:
            java_cevap = await foto_ilet(
                prediction_id=prediction_id,
                jwt=jwt.replace("Bearer ", "").strip(),
                dosya_adi=image.filename or "foto.jpg",
                icerik=icerik,
                content_type=image.content_type or "image/jpeg",
                oda_tipi=sonuc.oda_tipi,
                kalite_etiket=sonuc.kalite_etiket,
                kalite_skor=sonuc.kalite_skor,
                kalite_guven=sonuc.kalite_guven,
            )
        except Exception as e:
            java_hata = str(e)

    cevap = sonuc.to_dict()
    cevap["java_kayit"] = java_cevap
    cevap["java_hata"] = java_hata
    return cevap
