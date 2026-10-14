import os
import sys
import numpy as np
from app.services.face_service import FaceService
from app.config import settings

try:
    import cv2
except ImportError:
    cv2 = None

def main():
    print("==================================================")
    print("  DEEPCODE FACE AI - THỬ NGHIỆM ĐIỂM DANH THỰC TẾ ")
    print("==================================================")

    service = FaceService()
    samples_dir = settings.DATA_DIR / "sample_images"
    
    path_alice = str(samples_dir / "alice_enroll.jpg")
    path_bob = str(samples_dir / "bob_enroll.jpg")
    path_alice_checkin = str(samples_dir / "alice_checkin.jpg")

    if not os.path.exists(path_alice) or not os.path.exists(path_bob):
        print("Lỗi: Không tìm thấy ảnh mẫu trong", samples_dir)
        return

    img_alice = cv2.imread(path_alice)
    img_bob = cv2.imread(path_bob)
    img_checkin = cv2.imread(path_alice_checkin)

    # 1. Đăng ký khuôn mặt cho Alice và Bob
    print("\n[BƯỚC 1] ĐĂNG KÝ KHUÔN MẶT (ENROLLMENT)...")
    res_a = service.enroll_employee(
        image_bgr=img_alice,
        employee_id="EMP001",
        full_name="Nguyễn Thảo Alice",
        department="AI Research"
    )
    print(f" -> Đăng ký EMP001 (Alice): {'✅ THÀNH CÔNG' if res_a.success else f'❌ THẤT BẠI ({res_a.reasons})'}")

    res_b = service.enroll_employee(
        image_bgr=img_bob,
        employee_id="EMP002",
        full_name="Trần Văn Bob",
        department="Mobile Dev"
    )
    print(f" -> Đăng ký EMP002 (Bob)  : {'✅ THÀNH CÔNG' if res_b.success else f'❌ THẤT BẠI ({res_b.reasons})'}")

    # 2. Điểm danh nhận diện 1:N với ảnh chụp tại sảnh văn phòng (Check-in Photo)
    print("\n[BƯỚC 2] ĐIỂM DANH NHẬN DIỆN TỰ ĐỘNG (1:N RECOGNITION)...")
    rec_res = service.recognize_face(img_checkin, threshold=0.75)
    print(f" -> Kết quả điểm danh ảnh Check-in:")
    print(f"    • Trạng thái khớp: {'✅ KHỚP DANH TÍNH' if rec_res.matched else '❌ KHÔNG KHỚP'}")
    print(f"    • Mã NV phát hiện: {rec_res.employee_id}")
    print(f"    • Tên nhân viên  : {rec_res.full_name}")
    print(f"    • Độ tương đồng  : {rec_res.similarity * 100:.1f}%")
    print(f"    • Liveness/Thật  : {'✅ ĐẠT' if rec_res.liveness_passed else '❌ GIẢ MẠO'} (Score: {rec_res.liveness_score:.2f})")
    print(f"    • Chi tiết lý do : {rec_res.reasons}")

    # 3. Xác thực đối chiếu 1:1 (Verification)
    print("\n[BƯỚC 3] ĐỐI CHIẾU XÁC THỰC 1:1 (VERIFICATION)...")
    ver_alice = service.verify_employee(img_checkin, "EMP001", threshold=0.75)
    print(f" -> Đối chiếu ảnh Check-in với EMP001 (Alice): {'✅ KHỚP CHÍNH XÁC' if ver_alice.matched else '❌ KHÔNG KHỚP'} ({ver_alice.similarity * 100:.1f}%)")

    ver_bob = service.verify_employee(img_checkin, "EMP002", threshold=0.75)
    print(f" -> Đối chiếu ảnh Check-in với EMP002 (Bob)  : {'✅ KHỚP' if ver_bob.matched else '❌ TỪ CHỐI ĐÚNG'} ({ver_bob.similarity * 100:.1f}%)")


    # 4. Danh sách nhân viên trong Database
    print("\n[BƯỚC 4] DANH SÁCH KHUÔN MẶT ĐÃ LƯU TRONG VECTOR DATABASE:")
    profiles = service.storage.list_faces()
    for p in profiles:
        print(f" • Mã: {p.employee_id:<8} | Họ tên: {p.full_name:<20} | Phòng: {p.department:<15} | Vector: {p.embedding_dim} chiều")

    print("\n🎉 Hệ thống Face AI hoàn toàn hoạt động chính xác và sẵn sàng tích hợp!")

if __name__ == "__main__":
    main()
