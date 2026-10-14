import os
import numpy as np
from typing import List, Tuple, Optional
from app.config import settings
from app.schemas.models import BoundingBox, Landmark, FaceLandmarks, DetectedFace
from app.core.quality_checker import FaceQualityChecker

try:
    import cv2
except ImportError:
    cv2 = None

class FaceDetector:
    """
    Detects faces in images using OpenCV YuNet ONNX or Haar Cascade / DNN.
    Returns detected faces with bounding boxes, landmarks, and quality checks.
    """

    def __init__(self):
        self.yunet_detector = None
        self.cascade_detector = None
        self._init_models()

    def _init_models(self):
        if cv2 is None:
            return

        # Attempt to load YuNet if model file exists
        if os.path.exists(settings.YUNET_MODEL_PATH) and hasattr(cv2, 'FaceDetectorYN'):
            try:
                self.yunet_detector = cv2.FaceDetectorYN.create(
                    model=settings.YUNET_MODEL_PATH,
                    config="",
                    input_size=(320, 320),
                    score_threshold=0.6,
                    nms_threshold=0.3,
                    top_k=5000
                )
            except Exception as e:
                print(f"[FaceDetector] Could not load YuNet: {e}")
                self.yunet_detector = None

        # Fallback to Haar Cascade
        try:
            cascade_path = cv2.data.haarcascades + "haarcascade_frontalface_default.xml"
            if os.path.exists(cascade_path):
                self.cascade_detector = cv2.CascadeClassifier(cascade_path)
        except Exception as e:
            print(f"[FaceDetector] Haar cascade load warning: {e}")

    def detect_faces(self, image_bgr: np.ndarray) -> List[DetectedFace]:
        """
        Detects all faces in image_bgr.
        """
        if image_bgr is None or image_bgr.size == 0:
            return []

        h, w = image_bgr.shape[:2]
        detected: List[DetectedFace] = []

        # 1. Try YuNet ONNX detector
        if self.yunet_detector is not None:
            try:
                self.yunet_detector.setInputSize((w, h))
                _, faces = self.yunet_detector.detect(image_bgr)
                if faces is not None:
                    for face_data in faces:
                        # face_data format: [x, y, w, h, x_re, y_re, x_le, y_le, x_nt, y_nt, x_rcm, y_rcm, x_lcm, y_lcm, score]
                        bx, by, bw, bh = map(int, face_data[0:4])
                        score = float(face_data[-1])

                        # Landmarks
                        landmarks = FaceLandmarks(
                            right_eye=Landmark(x=float(face_data[4]), y=float(face_data[5])),
                            left_eye=Landmark(x=float(face_data[6]), y=float(face_data[7])),
                            nose_tip=Landmark(x=float(face_data[8]), y=float(face_data[9])),
                            mouth_right=Landmark(x=float(face_data[10]), y=float(face_data[11])),
                            mouth_left=Landmark(x=float(face_data[12]), y=float(face_data[13]))
                        )

                        bbox = BoundingBox(x=bx, y=by, width=bw, height=bh)
                        quality = FaceQualityChecker.check_quality(image_bgr, bbox)

                        detected.append(DetectedFace(
                            bbox=bbox,
                            landmarks=landmarks,
                            confidence=score,
                            quality=quality
                        ))
                    return detected
            except Exception as e:
                print(f"[FaceDetector] YuNet detection error: {e}")

        # 2. Fallback: Haar Cascade
        if self.cascade_detector is not None:
            gray = cv2.cvtColor(image_bgr, cv2.COLOR_BGR2GRAY)
            faces = self.cascade_detector.detectMultiScale(
                gray,
                scaleFactor=1.1,
                minNeighbors=5,
                minSize=(settings.MIN_FACE_SIZE, settings.MIN_FACE_SIZE)
            )

            for (x, y, bw, bh) in faces:
                bbox = BoundingBox(x=int(x), y=int(y), width=int(bw), height=int(bh))
                quality = FaceQualityChecker.check_quality(image_bgr, bbox)

                # Synthetic landmark estimation based on face anatomy ratios
                re_x = float(x + 0.3 * bw)
                re_y = float(y + 0.38 * bh)
                le_x = float(x + 0.7 * bw)
                le_y = float(y + 0.38 * bh)
                nose_x = float(x + 0.5 * bw)
                nose_y = float(y + 0.58 * bh)
                mr_x = float(x + 0.35 * bw)
                mr_y = float(y + 0.78 * bh)
                ml_x = float(x + 0.65 * bw)
                ml_y = float(y + 0.78 * bh)

                landmarks = FaceLandmarks(
                    right_eye=Landmark(x=re_x, y=re_y),
                    left_eye=Landmark(x=le_x, y=le_y),
                    nose_tip=Landmark(x=nose_x, y=nose_y),
                    mouth_right=Landmark(x=mr_x, y=mr_y),
                    mouth_left=Landmark(x=ml_x, y=ml_y)
                )

                detected.append(DetectedFace(
                    bbox=bbox,
                    landmarks=landmarks,
                    confidence=0.92,
                    quality=quality
                ))

        return detected
