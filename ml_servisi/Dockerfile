FROM python:3.11-slim

WORKDIR /app

# Sistem bağımlılıkları (Pillow için)
RUN apt-get update && apt-get install -y --no-install-recommends \
    libjpeg-dev zlib1g-dev \
    && rm -rf /var/lib/apt/lists/*

# Önce requirements (cache için)
COPY requirements.txt .
# CPU-only torch (Railway'de GPU yok, imaj küçülür)
RUN pip install --no-cache-dir torch==2.2.2 torchvision==0.17.2 --index-url https://download.pytorch.org/whl/cpu \
    && pip install --no-cache-dir -r requirements.txt

# Uygulama + modeller
COPY app/ ./app/
COPY model_dosyalari/ ./model_dosyalari/

EXPOSE 8000
CMD uvicorn app.main:app --host 0.0.0.0 --port ${PORT:-8000}
