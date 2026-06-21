"""
app/core/java_client.py
=======================
Doğrulanmış fotoğrafı + kalite sonucunu Java backend'e iletir.
Kullanıcının JWT'si pass-through edilir (mevcut auth korunur).

Java tarafında yeni bir endpoint bekler:
    POST /api/predictions/{predictionId}/images-from-ml
    Header: Authorization: Bearer <jwt>
    multipart/form-data:
        image: dosya
        odaTipi: "salon"
        kaliteEtiket: "Iyi"
        kaliteSkor: 2.0
        kaliteGuven: 0.87
"""

from __future__ import annotations

import os
import httpx

# Java backend taban URL'i — Railway'de internal veya public URL
JAVA_BASE_URL = os.getenv("JAVA_BASE_URL", "http://localhost:8080")
TIMEOUT = float(os.getenv("JAVA_TIMEOUT", "30"))


async def foto_ilet(
    prediction_id: str,
    jwt: str,
    dosya_adi: str,
    icerik: bytes,
    content_type: str,
    oda_tipi: str,
    kalite_etiket: str,
    kalite_skor: float,
    kalite_guven: float,
) -> dict:
    """Geçerli fotoğrafı Java'ya yükler. Java'nın döndürdüğü JSON'u verir."""
    url = f"{JAVA_BASE_URL}/api/predictions/{prediction_id}/images-from-ml"
    headers = {"Authorization": f"Bearer {jwt}"}

    files = {"image": (dosya_adi, icerik, content_type or "image/jpeg")}
    data = {
        "odaTipi": oda_tipi,
        "kaliteEtiket": kalite_etiket,
        "kaliteSkor": str(kalite_skor),
        "kaliteGuven": str(kalite_guven),
    }

    async with httpx.AsyncClient(timeout=TIMEOUT) as client:
        resp = await client.post(url, headers=headers, files=files, data=data)
        resp.raise_for_status()
        return resp.json()
