import os
import urllib.request
from pathlib import Path
from app.config import settings, MODELS_DIR

def download_file(url: str, dest_path: str):
    path = Path(dest_path)
    if path.exists():
        print(f"File already exists: {path}")
        return

    print(f"Downloading from {url} to {dest_path}...")
    try:
        urllib.request.urlretrieve(url, dest_path)
        print(f"Downloaded successfully: {dest_path} ({os.path.getsize(dest_path)} bytes)")
    except Exception as e:
        print(f"Error downloading {url}: {e}")

def main():
    Path(MODELS_DIR).mkdir(parents=True, exist_ok=True)
    print("=== Downloading Face AI Models (ONNX) ===")
    
    # Download YuNet Face Detector (lightweight, ~300KB)
    download_file(settings.YUNET_MODEL_URL, settings.YUNET_MODEL_PATH)

if __name__ == "__main__":
    main()
