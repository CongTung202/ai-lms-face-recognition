# ==========================================
# Dockerfile for Python Face AI Engine
# ==========================================

FROM python:3.11-slim

WORKDIR /app

# Install system dependencies required by OpenCV & ONNX Runtime
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    libgl1 \
    libglib2.0-0 \
    libgomp1 \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Copy requirements & install Python dependencies
COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

# Copy application source code
COPY . .

# Run model download script during build (or ensure directory exists)
RUN python download_models.py || true

# Set environment variables
ENV PYTHONUNBUFFERED=1
ENV MONGODB_URI=mongodb://mongo:27017/legiapos_db
ENV MONGODB_DB_NAME=legiapos_db

# Expose port
EXPOSE 8000

# Healthcheck
HEALTHCHECK --interval=30s --timeout=5s --start-period=15s --retries=3 \
  CMD curl -f http://127.0.0.1:8000/health || exit 1

# Start FastAPI server via Uvicorn
CMD ["python", "-m", "uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
