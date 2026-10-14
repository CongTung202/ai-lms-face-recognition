"""
Script dọn dẹp sạch toàn bộ cache dữ liệu khuôn mặt:
1. Dọn sạch file vector database: data/faces_vector_db.json
2. Xoá tất cả ảnh đăng ký: www/register/*.* và data/registered_faces/*.*
3. Dọn sạch dữ liệu khuôn mặt trong MongoDB (collection employees, dailyattendances)
"""
import os
import sys
import json
import shutil
from pathlib import Path

# Add project root to sys.path
BASE_DIR = Path(__file__).resolve().parent
sys.path.insert(0, str(BASE_DIR))

from app.config import settings
from app.core.face_storage import FaceStorage

def main():
    print("=" * 60)
    print("🧹 BẮT ĐẦU XOÁ TOÀN BỘ CACHE VÀ DỮ LIỆU KHUÔN MẶT...")
    print("=" * 60)

    # 1. Khởi tạo FaceStorage và xoá toàn diện trong MongoDB + RAM
    storage = FaceStorage()
    count = storage.clear_all(delete_images=True)

    # 2. Đảm bảo file JSON được làm mới hoàn toàn
    json_path = settings.DATA_DIR / "faces_vector_db.json"
    with open(json_path, "w", encoding="utf-8") as f:
        json.dump({"profiles": {}, "updated_at": None}, f, indent=2)

    # 3. Dọn sạch thư mục ảnh www/register
    reg_dir = settings.REGISTER_DIR
    reg_dir.mkdir(parents=True, exist_ok=True)
    for p in reg_dir.glob("*.*"):
        try:
            p.unlink()
        except Exception:
            pass

    # 4. Dọn sạch thư mục ảnh cũ data/registered_faces
    old_faces_dir = settings.DATA_DIR / "registered_faces"
    if old_faces_dir.exists():
        for p in old_faces_dir.glob("*.*"):
            try:
                p.unlink()
            except Exception:
                pass

    print(f"✅ Đã dọn sạch {count} hồ sơ khuôn mặt trong MongoDB & RAM!")
    print(f"✅ Đã làm mới file cache: {json_path}")
    print(f"✅ Đã dọn sạch thư mục ảnh đăng ký: {reg_dir}")
    print("=" * 60)
    print("🎉 HOÀN TẤT! Hệ thống đã sẵn sàng đăng ký khuôn mặt mới.")
    print("=" * 60)

if __name__ == "__main__":
    main()
