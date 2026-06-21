FROM python:3.11-slim

WORKDIR /app

# Sistem bağımlılıkları (Pillow için)
RUN apt-get update && apt-get install -y --no-install-recommends \
    libjpeg-dev zlib1g-dev \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .

# 1) ÖNCE numpy<2 sabitle (torch'tan ÖNCE, yoksa torch numpy 2.x çeker)
RUN pip install --no-cache-dir "numpy<2"

# 2) CPU-only torch (Railway'de GPU yok). numpy zaten 1.x sabit.
RUN pip install --no-cache-dir torch==2.2.2 torchvision==0.17.2 \
    --index-url https://download.pytorch.org/whl/cpu

# 3) Kalan bağımlılıklar
RUN pip install --no-cache-dir -r requirements.txt

# 4) numpy gerçekten <2 mi doğrula (build sırasında görürüz)
RUN python -c "import numpy; print('NumPy version:', numpy.__version__); assert numpy.__version__.startswith('1.'), 'NUMPY 2.x HALA YUKLU!'"

# Uygulama + modeller
COPY app/ ./app/
COPY model_dosyalari/ ./model_dosyalari/

EXPOSE 8000
CMD uvicorn app.main:app --host 0.0.0.0 --port ${PORT:-8000}
