import os
import sys
import argparse
import numpy as np
from app.services.face_service import FaceService
from app.config import settings

try:
    import cv2
except ImportError:
    cv2 = None

def load_image(image_path: str) -> np.ndarray:
    if not os.path.exists(image_path):
        print(f"Error: File not found: {image_path}")
        sys.exit(1)
    if cv2 is None:
        print("Error: OpenCV is not installed.")
        sys.exit(1)
    img = cv2.imread(image_path)
    if img is None:
        print(f"Error: Unable to decode image from {image_path}")
        sys.exit(1)
    return img

def cmd_enroll(args, service: FaceService):
    img = load_image(args.image)
    res = service.enroll_employee(
        image_bgr=img,
        employee_id=args.employee_id,
        full_name=args.name,
        department=args.dept
    )
    print("\n--- ENROLLMENT RESULT ---")
    print(f"Success       : {res.success}")
    print(f"Employee ID   : {res.employee_id}")
    print(f"Message       : {res.message}")
    print(f"Face Detected : {res.face_detected}")
    print(f"Quality Passed: {res.quality_passed}")
    if res.reasons:
        print(f"Reasons / Warnings: {', '.join(res.reasons)}")
    if res.image_saved_path:
        print(f"Saved Image   : {res.image_saved_path}")

def cmd_recognize(args, service: FaceService):
    img = load_image(args.image)
    res = service.recognize_face(image_bgr=img, threshold=args.threshold, check_liveness=not args.no_liveness)
    print("\n--- 1:N RECOGNITION RESULT ---")
    print(f"Matched       : {res.matched}")
    if res.matched:
        print(f"Employee ID   : {res.employee_id}")
        print(f"Full Name     : {res.full_name}")
        print(f"Similarity    : {res.similarity:.4f} (Threshold: {res.threshold})")
        print(f"Liveness Pass : {res.liveness_passed} (Score: {res.liveness_score:.3f})")
    else:
        print(f"Top Similarity: {res.similarity:.4f}")
        print(f"Reasons       : {', '.join(res.reasons)}")
        if res.candidates:
            print("\nTop Candidates:")
            for c in res.candidates:
                print(f" - {c.employee_id} ({c.full_name}): similarity {c.similarity:.4f}")

def cmd_verify(args, service: FaceService):
    img = load_image(args.image)
    res = service.verify_employee(image_bgr=img, employee_id=args.employee_id, threshold=args.threshold, check_liveness=not args.no_liveness)
    print("\n--- 1:1 VERIFICATION RESULT ---")
    print(f"Matched       : {res.matched}")
    print(f"Target ID     : {res.employee_id}")
    print(f"Full Name     : {res.full_name}")
    print(f"Similarity    : {res.similarity:.4f} (Threshold: {res.threshold})")
    print(f"Liveness Pass : {res.liveness_passed} (Score: {res.liveness_score:.3f})")
    if res.reasons:
        print(f"Notes         : {', '.join(res.reasons)}")

def cmd_list(args, service: FaceService):
    profiles = service.storage.list_faces()
    print(f"\n--- REGISTERED EMPLOYEES ({len(profiles)}) ---")
    if not profiles:
        print("No registered face profiles found.")
        return
    for p in profiles:
        print(f"• ID: {p.employee_id:<10} | Name: {p.full_name or 'N/A':<20} | Dept: {p.department or 'N/A':<15} | Vector Dim: {p.embedding_dim}")

def cmd_delete(args, service: FaceService):
    success = service.storage.delete_face(args.employee_id)
    if success:
        print(f"Successfully deleted face profile for '{args.employee_id}'.")
    else:
        print(f"Employee ID '{args.employee_id}' not found.")

def main():
    parser = argparse.ArgumentParser(description="DeepCode Face Recognition CLI Utility")
    subparsers = parser.add_subparsers(dest="command", help="Command to run")

    # Enroll
    p_enroll = subparsers.add_parser("enroll", help="Enroll a new face profile")
    p_enroll.add_argument("image", help="Path to face image")
    p_enroll.add_argument("employee_id", help="Unique employee ID")
    p_enroll.add_argument("--name", help="Employee full name", default=None)
    p_enroll.add_argument("--dept", help="Department", default=None)

    # Recognize
    p_rec = subparsers.add_parser("recognize", help="Recognize face in image (1:N search)")
    p_rec.add_argument("image", help="Path to query image")
    p_rec.add_argument("--threshold", type=float, default=settings.SIMILARITY_THRESHOLD, help="Cosine similarity threshold")
    p_rec.add_argument("--no-liveness", action="store_true", help="Disable anti-spoofing check")

    # Verify
    p_ver = subparsers.add_parser("verify", help="Verify face matches claimed employee ID (1:1)")
    p_ver.add_argument("image", help="Path to query image")
    p_ver.add_argument("employee_id", help="Claimed employee ID")
    p_ver.add_argument("--threshold", type=float, default=settings.SIMILARITY_THRESHOLD, help="Cosine similarity threshold")
    p_ver.add_argument("--no-liveness", action="store_true", help="Disable anti-spoofing check")

    # List
    subparsers.add_parser("list", help="List all enrolled employee faces")

    # Delete
    p_del = subparsers.add_parser("delete", help="Delete face profile for an employee")
    p_del.add_argument("employee_id", help="Employee ID to delete")

    args = parser.parse_args()
    if not args.command:
        parser.print_help()
        return

    service = FaceService()

    if args.command == "enroll":
        cmd_enroll(args, service)
    elif args.command == "recognize":
        cmd_recognize(args, service)
    elif args.command == "verify":
        cmd_verify(args, service)
    elif args.command == "list":
        cmd_list(args, service)
    elif args.command == "delete":
        cmd_delete(args, service)

if __name__ == "__main__":
    main()
