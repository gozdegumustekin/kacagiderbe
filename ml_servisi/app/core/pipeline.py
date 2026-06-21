"""
app/core/pipeline.py
====================
3 aşamalı fotoğraf doğrulama + kalite değerlendirme hattı.

AŞAMALAR
--------
1. inside_outside     → "outside" ise (dış cephe, bahçe) RED
2. oda_kategorileri   → "belirsiz" ise (oda değil) RED
                        VEYA beklenen oda ≠ tahmin (salon beklenirken mutfak) RED
3. kalite_<oda>       → Kotu/Normal/Iyi → kalite skoru

NOT: room_notroom ayrı model değildir. "Oda mı, değil mi" kararını
oda_kategorileri modelinin 'belirsiz' sınıfı verir.

Her aşama bir AsamaSonuc döndürür; pipeline ilk RED'de durur.
"""

from __future__ import annotations

from dataclasses import dataclass, asdict
from typing import Optional

from PIL import Image

from app.core import models


# Beklenen oda tipi → hangi kalite modeli kullanılacak
ODA_KALITE_MODELI = {
    "salon": "kalite_salon",
    "mutfak": "kalite_mutfak",
    "banyo": "kalite_banyo",
}

# oda_kategorileri modelinin çıktısı → bizim beklediğimiz tip eşleşmesi.
# Model "salon/oda/mutfak/banyo/balkon" döndürür; beklenen tiple karşılaştırılır.
# Eğer model farklı etiketler kullanıyorsa burada eşleştir.
ODA_ESLESME = {
    "salon": {"saloon_bedroom"},
    "mutfak": {"kitchen"},
    "banyo": {"bathroom_toilet"},
}

# oda_kategorileri modelinin "oda değil" anlamına gelen sınıf etiketi.
# models.py'deki classes listesinde bu etiket BİREBİR aynı olmalı.
# Eğitimdeki klasör adın neyse onu yaz: "belirsiz" / "diger" / "notroom"...
BELIRSIZ_ETIKET = "Not_Room_Other"

# Düşük güven eşiği — model çok kararsızsa uyarı verebiliriz (opsiyonel)
MIN_GUVEN = 0.5  # şimdilik kapalı; istersen 0.5 yap


@dataclass
class AsamaSonuc:
    asama: int
    asama_adi: str
    gecti: bool
    tahmin: str
    guven: float
    mesaj: str


@dataclass
class DogrulamaSonuc:
    gecerli: bool
    red_asamasi: Optional[int]
    asamalar: list
    # geçerliyse dolu:
    oda_tipi: Optional[str] = None
    kalite_etiket: Optional[str] = None
    kalite_skor: Optional[float] = None
    kalite_guven: Optional[float] = None
    mesaj: str = ""

    def to_dict(self):
        return {
            "gecerli": self.gecerli,
            "red_asamasi": self.red_asamasi,
            "asamalar": [asdict(a) for a in self.asamalar],
            "oda_tipi": self.oda_tipi,
            "kalite_etiket": self.kalite_etiket,
            "kalite_skor": self.kalite_skor,
            "kalite_guven": self.kalite_guven,
            "mesaj": self.mesaj,
        }


# Kalite etiketi → ordinal skor (ortalama almak için)
KALITE_ORDINAL = {"Kotu": 0.0, "Normal": 1.0, "Iyi": 2.0}
ORDINAL_KALITE = {0: "Kotu", 1: "Normal", 2: "Iyi"}


def kalite_ortalama(skorlar: list[float]) -> tuple[str, float]:
    """Ordinal kalite skorlarının ortalamasını alıp etikete çevirir.
    Dönen: (etiket, ortalama_skor)."""
    if not skorlar:
        return "bilinmiyor", 0.0
    ort = sum(skorlar) / len(skorlar)
    yuvarlanmis = round(ort)
    yuvarlanmis = max(0, min(2, yuvarlanmis))
    return ORDINAL_KALITE[yuvarlanmis], ort


class DogrulamaHatti:
    """Tek bir fotoğrafı 4 aşamadan geçirir.
    İlerleme bildirimi için her aşamada callback çağırır (WebSocket)."""

    def __init__(self, on_asama=None):
        # on_asama(AsamaSonuc) — her aşama bitince çağrılır (opsiyonel)
        self.on_asama = on_asama

    def _bildir(self, sonuc: AsamaSonuc):
        if self.on_asama:
            self.on_asama(sonuc)

    def calistir(self, image: Image.Image, beklenen_tip: str) -> DogrulamaSonuc:
        beklenen_tip = (beklenen_tip or "").strip().lower()
        if beklenen_tip not in ODA_KALITE_MODELI:
            return DogrulamaSonuc(
                gecerli=False, red_asamasi=0, asamalar=[],
                mesaj=f"Geçersiz oda tipi: '{beklenen_tip}'. "
                      f"Beklenen: {list(ODA_KALITE_MODELI.keys())}")

        asamalar = []

        # ── Aşama 1: inside / outside ──
        r1 = models.model_al("inside_outside").predict(image)
        gecti1 = r1["label"].lower() == "inside"
        a1 = AsamaSonuc(1, "inside_outside", gecti1, r1["label"], r1["confidence"],
                        "İç mekan ✓" if gecti1
                        else "Bu bir dış mekan/cephe fotoğrafı gibi görünüyor. "
                             "Lütfen oda içi fotoğraf yükleyin.")
        asamalar.append(a1)
        self._bildir(a1)
        if not gecti1:
            return DogrulamaSonuc(False, 1, asamalar, mesaj=a1.mesaj)

        # ── Aşama 2: oda kategorisi — hem "oda mı?" hem "doğru oda mı?" ──
        r2 = models.model_al("oda_kategorileri").predict(image)
        tahmin_oda = r2["label"].lower()

        # 2a) "belirsiz" → oda değil, RED
        if tahmin_oda == BELIRSIZ_ETIKET.lower():
            a2 = AsamaSonuc(2, "oda_kategorileri", False, r2["label"], r2["confidence"],
                            "Bu fotoğraf bir oda gibi görünmüyor (asansör, koridor, "
                            "plan, belirsiz görüntü vb.). Lütfen oda fotoğrafı yükleyin.")
            asamalar.append(a2)
            self._bildir(a2)
            return DogrulamaSonuc(False, 2, asamalar, mesaj=a2.mesaj)

        # 2b) Oda ama beklenenle eşleşiyor mu? (salon beklenirken mutfak)
        kabul = ODA_ESLESME.get(beklenen_tip, {beklenen_tip})
        gecti2 = tahmin_oda in kabul
        a2 = AsamaSonuc(2, "oda_kategorileri", gecti2, r2["label"], r2["confidence"],
                        f"{beklenen_tip.capitalize()} ✓" if gecti2
                        else f"'{beklenen_tip}' bekleniyordu ama fotoğraf "
                             f"'{tahmin_oda}' gibi görünüyor. Lütfen doğru oda "
                             f"fotoğrafı yükleyin.")
        asamalar.append(a2)
        self._bildir(a2)
        if not gecti2:
            return DogrulamaSonuc(False, 2, asamalar, mesaj=a2.mesaj)

        # ── Aşama 3: kalite ──
        kalite_model_adi = ODA_KALITE_MODELI[beklenen_tip]
        r3 = models.model_al(kalite_model_adi).predict(image)
        kalite_etiket = r3["label"]
        a3 = AsamaSonuc(3, "kalite", True, kalite_etiket, r3["confidence"],
                        f"Kalite: {kalite_etiket}")
        asamalar.append(a3)
        self._bildir(a3)

        return DogrulamaSonuc(
            gecerli=True,
            red_asamasi=None,
            asamalar=asamalar,
            oda_tipi=beklenen_tip,
            kalite_etiket=kalite_etiket,
            kalite_skor=KALITE_ORDINAL.get(kalite_etiket, 1.0),
            kalite_guven=r3["confidence"],
            mesaj=f"Fotoğraf geçerli. Kalite: {kalite_etiket}")
