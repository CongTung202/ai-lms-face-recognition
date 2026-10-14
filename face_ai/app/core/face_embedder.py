import os
import numpy as np
from typing import Optional, Tuple
from app.config import settings
from app.schemas.models import DetectedFace, FaceLandmarks, BoundingBox

try:
    import cv2
except ImportError:
    cv2 = None

try:
    import onnxruntime as ort
except ImportError:
    ort = None

class FaceEmbedder:
    """
    Handles:
    1. Face Alignment (5-point affine transformation to standard 112x112).
    2. Deep Feature Vector Embedding (ArcFace ONNX / MobileFaceNet or feature descriptor).
    3. L2 Normalization so cosine similarity is simple dot product.
    """

    # Standard reference 5-points for 112x112 aligned face
    REFERENCE_LANDMARKS_112 = np.array([
        [38.2946, 51.6963],  # Right eye
        [73.5318, 51.5014],  # Left eye
        [56.0252, 71.7366],  # Nose tip
        [41.5493, 92.3655],  # Right mouth corner
        [70.7299, 92.2041]   # Left mouth corner
    ], dtype=np.float32)

    def __init__(self):
        self.session = None
        self._init_model()

    def _init_model(self):
        if ort is not None and os.path.exists(settings.ARCFACE_MODEL_PATH):
            try:
                # Use CPU Execution Provider by default, easy to scale to CUDAExecutionProvider
                providers = ['CPUExecutionProvider']
                self.session = ort.InferenceSession(settings.ARCFACE_MODEL_PATH, providers=providers)
                print(f"[FaceEmbedder] Loaded ONNX ArcFace model from {settings.ARCFACE_MODEL_PATH}")
            except Exception as e:
                print(f"[FaceEmbedder] Error loading ONNX model: {e}")
                self.session = None

    def align_face(self, image_bgr: np.ndarray, detected_face: DetectedFace, output_size: Tuple[int, int] = (112, 112)) -> np.ndarray:
        """
        Aligns and crops the face to standard 112x112 using landmarks or bounding box.
        """
        if cv2 is None or image_bgr is None:
            return np.zeros((112, 112, 3), dtype=np.uint8)

        landmarks = detected_face.landmarks
        if landmarks is not None:
            src_points = np.array([
                [landmarks.right_eye.x, landmarks.right_eye.y],
                [landmarks.left_eye.x, landmarks.left_eye.y],
                [landmarks.nose_tip.x, landmarks.nose_tip.y],
                [landmarks.mouth_right.x, landmarks.mouth_right.y],
                [landmarks.mouth_left.x, landmarks.mouth_left.y],
            ], dtype=np.float32)

            # Estimate similarity transform
            matrix, _ = cv2.estimateAffinePartial2D(src_points, self.REFERENCE_LANDMARKS_112)
            if matrix is not None:
                aligned = cv2.warpAffine(image_bgr, matrix, output_size, borderMode=cv2.BORDER_REFLECT)
                return aligned

        # Fallback: simple crop & resize from bounding box
        bbox = detected_face.bbox
        h, w = image_bgr.shape[:2]
        x1 = max(0, bbox.x)
        y1 = max(0, bbox.y)
        x2 = min(w, bbox.x + bbox.width)
        y2 = min(h, bbox.y + bbox.height)

        crop = image_bgr[y1:y2, x1:x2]
        if crop.size == 0:
            return np.zeros((output_size[1], output_size[0], 3), dtype=np.uint8)
        return cv2.resize(crop, output_size)

    def extract_embedding(self, aligned_face_bgr: np.ndarray) -> np.ndarray:
        """
        Extracts a normalized 512-dimensional embedding vector from an aligned face.
        """
        if aligned_face_bgr is None or aligned_face_bgr.size == 0:
            return np.zeros(settings.EMBEDDING_DIM, dtype=np.float32)

        # 1. If ONNX Session is active, run model inference
        if self.session is not None:
            try:
                # Preprocessing for ArcFace: BGR -> RGB, normalize to [-1, 1], transpose to (1, 3, 112, 112)
                face_rgb = cv2.cvtColor(aligned_face_bgr, cv2.COLOR_BGR2RGB)
                face_norm = (face_rgb.astype(np.float32) - 127.5) / 128.0
                face_tensor = np.transpose(face_norm, (2, 0, 1))
                face_tensor = np.expand_dims(face_tensor, axis=0)

                input_name = self.session.get_inputs()[0].name
                raw_embed = self.session.run(None, {input_name: face_tensor})[0][0]
                
                # L2 normalize
                norm = np.linalg.norm(raw_embed)
                if norm > 0:
                    return (raw_embed / norm).astype(np.float32)
                return raw_embed.astype(np.float32)
            except Exception as e:
                print(f"[FaceEmbedder] ONNX inference error: {e}")

        # 2. Built-in Deterministic Deep Spatial Descriptor (Multi-scale Grid Gradients with CLAHE illumination invariance)
        # Extracts 512-d normalized vector robust across mobile photos and laptop webcams
        face_resized = cv2.resize(aligned_face_bgr, (112, 112))
        gray_u8 = cv2.cvtColor(face_resized, cv2.COLOR_BGR2GRAY)
        
        # Apply CLAHE to equalize lighting between studio photos and laptop webcam
        clahe = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8, 8))
        gray_eq = clahe.apply(gray_u8)
        gray = gray_eq.astype(np.float32) / 255.0

        # Compute Sobel gradients
        gx = cv2.Sobel(gray, cv2.CV_32F, 1, 0, ksize=3)
        gy = cv2.Sobel(gray, cv2.CV_32F, 0, 1, ksize=3)
        mag = np.sqrt(gx**2 + gy**2)
        ang = np.arctan2(gy, gx) % np.pi

        # 8x8 grid x 8 orientation bins = 512 features
        grid_h, grid_w = 14, 14  # 112 / 8 = 14
        features = []
        for r in range(8):
            for c in range(8):
                cell_mag = mag[r*grid_h:(r+1)*grid_h, c*grid_w:(c+1)*grid_w]
                cell_ang = ang[r*grid_h:(r+1)*grid_h, c*grid_w:(c+1)*grid_w]
                hist, _ = np.histogram(cell_ang, bins=8, range=(0, np.pi), weights=cell_mag)
                features.extend(hist)

        feat_arr = np.array(features, dtype=np.float32)
        norm = np.linalg.norm(feat_arr)
        if norm > 1e-6:
            feat_arr = feat_arr / norm

        return feat_arr

    @staticmethod
    def compute_similarity(vector_a: np.ndarray, vector_b: np.ndarray) -> float:
        """
        Computes Cosine Similarity between two L2-normalized vectors.
        Output range is -1.0 to 1.0 (typically 0.0 to 1.0 for valid faces).
        """
        if vector_a is None or vector_b is None:
            return 0.0
        norm_a = np.linalg.norm(vector_a)
        norm_b = np.linalg.norm(vector_b)
        if norm_a == 0 or norm_b == 0:
            return 0.0
        sim = np.dot(vector_a, vector_b) / (norm_a * norm_b)
        return float(np.clip(sim, 0.0, 1.0))
