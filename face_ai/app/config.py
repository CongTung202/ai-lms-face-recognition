import os
from pathlib import Path
try:
    from dotenv import load_dotenv
except ImportError:
    load_dotenv = None

try:
    from pydantic import BaseModel
except ImportError:
    from dataclasses import dataclass
    class BaseModel:
        pass


BASE_DIR = Path(__file__).resolve().parent.parent
ROOT_DIR = BASE_DIR.parent
DATA_DIR = BASE_DIR / "data"
MODELS_DIR = DATA_DIR / "models"
FACES_DIR = DATA_DIR / "registered_faces"
REGISTER_DIR = BASE_DIR / "www" / "register"
DB_PATH = DATA_DIR / "faces_vector_db.json"

# Nạp file .env từ thư mục gốc dự án (hoặc fallback về thư mục cục bộ)
if load_dotenv:
    root_env_file = ROOT_DIR / ".env"
    local_env_file = BASE_DIR / ".env"
    if root_env_file.exists():
        load_dotenv(dotenv_path=root_env_file, override=True)
    elif local_env_file.exists():
        load_dotenv(dotenv_path=local_env_file, override=True)

# Ensure core directories exist
DATA_DIR.mkdir(parents=True, exist_ok=True)
MODELS_DIR.mkdir(parents=True, exist_ok=True)

class Settings(BaseModel):
    # Paths
    BASE_DIR: Path = BASE_DIR
    DATA_DIR: Path = DATA_DIR
    MODELS_DIR: Path = MODELS_DIR
    FACES_DIR: Path = FACES_DIR
    REGISTER_DIR: Path = REGISTER_DIR
    DB_PATH: Path = DB_PATH

    # App Settings
    APP_NAME: str = "DeepCode Face AI Service"
    APP_VERSION: str = "1.0.0"
    DEBUG: bool = True
    
    # Face Quality Thresholds
    MIN_FACE_SIZE: int = 50          # Minimum width & height of detected face in pixels
    MIN_FACE_RATIO: float = 0.008    # Face area must be at least 0.8% of whole frame (optimized for wide selfie mobile cameras)
    MIN_BRIGHTNESS: float = 15.0     # Mean grayscale brightness (0-255, relaxed for low-light/mobile)
    MAX_BRIGHTNESS: float = 245.0    # Avoid overexposure
    MIN_BLUR_SCORE: float = 2.5      # Laplacian variance threshold on normalized 160x160 crop (laptop webcams)
    
    # Face Recognition Thresholds
    SIMILARITY_THRESHOLD: float = 0.60  # Cosine similarity threshold for verification (0.0 to 1.0)
    EMBEDDING_DIM: int = 512            # Vector dimension (512 for ArcFace / 128 for MobileFaceNet)
    
    # Liveness Thresholds
    LIVENESS_THRESHOLD: float = 0.55    # Liveness confidence score threshold (calibrated for laptop webcams)
    
    # Model Configurations
    YUNET_MODEL_URL: str = "https://github.com/opencv/opencv_zoo/raw/main/models/face_detection_yunet/face_detection_yunet_2023mar.onnx"
    ARCFACE_MODEL_URL: str = "https://github.com/onnx/models/raw/main/validated/vision/body_analysis/arcface/model/arcfaceresnet100-8.onnx"
    
    YUNET_MODEL_PATH: str = str(MODELS_DIR / "face_detection_yunet.onnx")
    ARCFACE_MODEL_PATH: str = str(
        (MODELS_DIR / "w600k_mbf.onnx") if (MODELS_DIR / "w600k_mbf.onnx").exists()
        else (MODELS_DIR / "arcface_resnet100.onnx") if (MODELS_DIR / "arcface_resnet100.onnx").exists()
        else (MODELS_DIR / "face_recognition_sface.onnx")
    )
    
    # Storage & Database (MongoDB)
    STORAGE_TYPE: str = os.getenv("STORAGE_TYPE", "mongodb") # 'mongodb', 'local', or 'qdrant'
    MONGODB_URI: str = os.getenv("MONGODB_URI") or os.getenv("MONGO_URI") or "mongodb://localhost:27017/crm_deepcode_db"
    MONGODB_DB_NAME: str = os.getenv("MONGODB_DB_NAME", "crm_deepcode_db")
    USE_MONGODB: bool = True
    QDRANT_URL: str = "http://localhost:6333"
    QDRANT_COLLECTION: str = "employee_faces"

    # Microservice Security (Node.js <-> Python HMAC Authentication)
    INTERNAL_AI_SECRET: str = os.getenv("INTERNAL_AI_SECRET", "FaceAI_Internal_Microservice_Secret_Key_999")
    REQUIRE_HMAC_AUTH: bool = os.getenv("REQUIRE_HMAC_AUTH", "true").lower() in ("true", "1", "yes")

settings = Settings()

