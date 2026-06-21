"""
app/core/models.py
==================
Tüm ResNet18 (.pth) modellerini yükler ve tahmin sunar.

ÖNEMLİ — TRANSFORM TUTARLILIĞI
------------------------------
Buradaki inference transform'u, modeli EĞİTİRKEN kullandığın
VALIDATION transform'u ile BİREBİR aynı olmalı. Eğitim scriptlerinde
(model_egit_*_cat.py) val_transform genelde şöyledir:

    transforms.Compose([
        transforms.Resize((224, 224)),
        transforms.ToTensor(),
        transforms.Normalize([0.485, 0.456, 0.406], [0.229, 0.224, 0.225])
    ])

Eğer senin val_transform'un FARKLI ise (örn. Resize(256)+CenterCrop(224)),
aşağıdaki INFERENCE_TRANSFORM'u ona göre düzelt — yoksa accuracy düşer.
"""

from __future__ import annotations

import io
from pathlib import Path
from typing import Dict, List

import torch
import torch.nn as nn
from PIL import Image
from torchvision import models, transforms

# ────────────────────────────────────────────────────────────
#  AYARLAR
# ────────────────────────────────────────────────────────────
MODEL_DIR = Path(__file__).resolve().parent.parent.parent / "model_dosyalari"
DEVICE = torch.device("cuda" if torch.cuda.is_available() else "cpu")

# Inference transform — EĞİTİMDEKİ VAL TRANSFORM İLE AYNI OLMALI
INFERENCE_TRANSFORM = transforms.Compose([
    transforms.Resize((224, 224)),
    transforms.ToTensor(),
    transforms.Normalize([0.485, 0.456, 0.406], [0.229, 0.224, 0.225]),
])

# Her modelin dosya adı ve sınıf etiketleri.
# DİKKAT: classes listesi, eğitimde ImageFolder'ın ALFABETİK sıraladığı
# sınıf isimleriyle BİREBİR AYNI SIRADA olmalı (PyTorch böyle indeksler).
# Senin banyo çıktında: ['Iyi', 'Kotu', 'Normal'] — alfabetik, doğru.
MODEL_TANIMLARI: Dict[str, dict] = {
    "inside_outside": {
        "dosya": "resnet18_inside_outside.pth",
        "classes": ["inside", "outside"],   # ← eğitimdeki klasör adlarına göre düzelt
    },
    # NOT: room_notroom AYRI MODEL DEĞİL. "Oda mı, değil mi" kararını
    # oda_kategorileri modeli kendi 'belirsiz' sınıfıyla verir (aşağıya bak).
    "oda_kategorileri": {
        "dosya": "resnet18_oda_kategorileri.pth",
        # ← eğitimdeki sınıflar (ALFABETİK sırayla, ImageFolder böyle indeksler).
        # 'belirsiz' sınıfı = "bu bir oda değil" demek. Eğitimdeki klasör adın
        # neyse ("belirsiz" / "diger" / "notroom") onu BURAYA yaz ve aşağıdaki
        # BELIRSIZ_ETIKET sabitini de aynı yap.
        "classes": ["Bathroom_Toilet","Kitchen","Not_Room_Other","Saloon_Bedroom"],
    },
    "kalite_salon": {
        "dosya": "resnet18_kalite_salon.pth",
        "classes": ["Iyi", "Kotu", "Normal"],
    },
    "kalite_mutfak": {
        "dosya": "resnet18_kalite_mutfak.pth",
        "classes": ["Iyi", "Kotu", "Normal"],
    },
    "kalite_banyo": {
        "dosya": "resnet18_kalite_banyo.pth",
        "classes": ["Iyi", "Kotu", "Normal"],
    },
}


# ────────────────────────────────────────────────────────────
#  MODEL SARMALAYICI
# ────────────────────────────────────────────────────────────
class YukluModel:
    def __init__(self, model: nn.Module, classes: List[str]):
        self.model = model
        self.classes = classes

    @torch.no_grad()
    def predict(self, image: Image.Image) -> dict:
        """Tek görüntü için tahmin. {label, index, confidence, probs} döner."""
        x = INFERENCE_TRANSFORM(image.convert("RGB")).unsqueeze(0).to(DEVICE)
        logits = self.model(x)
        probs = torch.softmax(logits, dim=1)[0]
        conf, idx = torch.max(probs, dim=0)
        return {
            "label": self.classes[int(idx)],
            "index": int(idx),
            "confidence": float(conf),
            "probs": {self.classes[i]: float(probs[i]) for i in range(len(self.classes))},
        }


def _resnet18_yukle(dosya_yolu: Path, sinif_sayisi: int) -> nn.Module:
    """ResNet18 iskeleti oluşturup .pth ağırlıklarını yükler.
    Eğitimde fc katmanı sinif_sayisi'na göre değiştirilmişti, burada da aynısı."""
    model = models.resnet18(weights=None)
    model.fc = nn.Linear(model.fc.in_features, sinif_sayisi)

    state = torch.load(dosya_yolu, map_location=DEVICE)
    # Bazı kayıtlar {'model_state_dict': ...} sarmalı olabilir
    if isinstance(state, dict) and "model_state_dict" in state:
        state = state["model_state_dict"]
    model.load_state_dict(state)
    model.to(DEVICE)
    model.eval()
    return model


# ────────────────────────────────────────────────────────────
#  GLOBAL MODEL DEPOSU
# ────────────────────────────────────────────────────────────
_MODELLER: Dict[str, YukluModel] = {}


def modelleri_yukle() -> None:
    """Startup'ta tüm modelleri belleğe yükler."""
    print(f"📦 Modeller yükleniyor (device={DEVICE})...")
    for ad, tanim in MODEL_TANIMLARI.items():
        yol = MODEL_DIR / tanim["dosya"]
        if not yol.exists():
            print(f"  ⚠️  {ad}: dosya yok ({yol}), atlanıyor.")
            continue
        try:
            model = _resnet18_yukle(yol, len(tanim["classes"]))
            _MODELLER[ad] = YukluModel(model, tanim["classes"])
            print(f"  ✅ {ad} yüklendi ({len(tanim['classes'])} sınıf).")
        except Exception as e:
            print(f"  ❌ {ad} yüklenemedi: {e}")
    print(f"📦 Toplam {len(_MODELLER)} model yüklü.")


def model_al(ad: str) -> YukluModel:
    if ad not in _MODELLER:
        raise KeyError(f"Model yüklü değil: {ad}. Yüklü olanlar: {list(_MODELLER.keys())}")
    return _MODELLER[ad]


def model_var_mi(ad: str) -> bool:
    return ad in _MODELLER


def goruntu_oku(icerik: bytes) -> Image.Image:
    """Byte içeriğinden PIL Image üretir."""
    return Image.open(io.BytesIO(icerik))


def yuklu_modeller() -> List[str]:
    return list(_MODELLER.keys())