import os
import sys
import numpy as np
from app.config import settings
from app.core.quality_checker import FaceQualityChecker
from app.core.face_detector import FaceDetector
from app.core.face_embedder import FaceEmbedder
from app.core.liveness_checker import LivenessChecker
from app.core.face_storage import FaceStorage
from app.services.face_service import FaceService
from app.schemas.models import BoundingBox, DetectedFace, Landmark, FaceLandmarks

def test_quality_checker():
    print("Testing Quality Checker...")
    img = np.full((300, 300, 3), 180, dtype=np.uint8)
    bbox = BoundingBox(x=50, y=50, width=150, height=150)
    
    quality = FaceQualityChecker.check_quality(img, bbox)
    assert quality.brightness > 0, "Brightness should be > 0"
    assert quality.face_ratio > 0.015, "Face ratio should be > 0.015"
    print(" -> Quality Checker PASSED")

def test_face_alignment_and_embedding():
    print("Testing Face Alignment & 512-d Embedding...")
    # Image with spatial gradients and patterns
    img = np.random.randint(50, 220, (300, 300, 3), dtype=np.uint8)
    embedder = FaceEmbedder()

    
    bbox = BoundingBox(x=50, y=50, width=150, height=150)
    detected = DetectedFace(
        bbox=bbox,
        landmarks=FaceLandmarks(
            right_eye=Landmark(x=90, y=100),
            left_eye=Landmark(x=160, y=100),
            nose_tip=Landmark(x=125, y=130),
            mouth_right=Landmark(x=95, y=165),
            mouth_left=Landmark(x=155, y=165)
        ),
        confidence=0.95,
        quality=FaceQualityChecker.check_quality(img, bbox)
    )

    aligned = embedder.align_face(img, detected)
    assert aligned.shape == (112, 112, 3), f"Aligned face should be (112, 112, 3), got {aligned.shape}"

    embedding = embedder.extract_embedding(aligned)
    assert len(embedding) == settings.EMBEDDING_DIM, f"Embedding dimension should be {settings.EMBEDDING_DIM}"
    norm = np.linalg.norm(embedding)
    assert abs(norm - 1.0) < 1e-3, "Embedding should be L2 normalized"
    print(" -> Face Alignment & Embedding PASSED")

def test_storage_and_search():
    print("Testing Vector Storage & Cosine Search...")
    test_db = settings.DATA_DIR / "test_faces_db.json"
    storage = FaceStorage(db_file=test_db)
    storage.profiles = {}

    vec1 = np.random.randn(512).astype(np.float32)
    vec1 /= np.linalg.norm(vec1)

    storage.save_face(
        employee_id="EMP_TEST",
        embedding=vec1,
        full_name="Unit Test Employee",
        department="Quality Assurance"
    )

    profile = storage.get_face("EMP_TEST")
    assert profile is not None, "Profile should be retrievable"
    assert profile["full_name"] == "Unit Test Employee"

    candidates = storage.search_similar_faces(vec1, top_k=1)
    assert len(candidates) == 1, "Should find 1 candidate"
    assert candidates[0].employee_id == "EMP_TEST"
    assert candidates[0].similarity > 0.99, "Similarity of identical vector should be ~1.0"

    storage.delete_face("EMP_TEST")
    assert storage.get_face("EMP_TEST") is None
    if test_db.exists():
        os.remove(test_db)
    print(" -> Vector Storage & Cosine Search PASSED")

def main():
    print("========================================")
    print("      RUNNING FACE AI UNIT TESTS        ")
    print("========================================")
    test_quality_checker()
    test_face_alignment_and_embedding()
    test_storage_and_search()
    print("\n✅ ALL UNIT TESTS COMPLETED SUCCESSFULLY!")

if __name__ == "__main__":
    main()
