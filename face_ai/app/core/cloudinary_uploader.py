import cv2
import numpy as np
import logging
from typing import Optional, Union
import cloudinary
import cloudinary.uploader
from app.config import settings

logger = logging.getLogger(__name__)

# Initialize Cloudinary SDK
cloudinary.config(
    cloud_name=settings.CLOUDINARY_CLOUD_NAME,
    api_key=settings.CLOUDINARY_API_KEY,
    api_secret=settings.CLOUDINARY_API_SECRET,
    secure=True
)

class CloudinaryUploader:
    """
    Tải ảnh chân dung khuôn mặt sinh viên lên dịch vụ Cloudinary
    Lưu trữ an toàn, truy xuất nhanh qua CDN quốc tế
    """

    @staticmethod
    def upload_face_image(
        image_data: Union[np.ndarray, bytes],
        student_id: str,
        folder: Optional[str] = None
    ) -> Optional[str]:
        """
        Uploads face image (BGR numpy array or bytes) to Cloudinary.
        Returns secure HTTPS URL if success, else None.
        """
        if image_data is None:
            return None

        upload_folder = folder or settings.CLOUDINARY_FOLDER
        public_id = f"{student_id}_face_primary"

        try:
            # 1. Convert BGR numpy array to JPEG byte buffer
            if isinstance(image_data, np.ndarray):
                success, buffer = cv2.imencode(".jpg", image_data, [cv2.IMWRITE_JPEG_QUALITY, 92])
                if not success:
                    logger.error(f"Failed to encode image to JPEG for {student_id}")
                    return None
                image_bytes = buffer.tobytes()
            else:
                image_bytes = image_data

            # 2. Upload to Cloudinary
            response = cloudinary.uploader.upload(
                image_bytes,
                folder=upload_folder,
                public_id=public_id,
                overwrite=True,
                resource_type="image",
                transformation=[
                    {"quality": "auto:good"},
                    {"fetch_format": "auto"}
                ]
            )

            secure_url = response.get("secure_url")
            logger.info(f"Successfully uploaded face for {student_id} to Cloudinary: {secure_url}")
            return secure_url

        except Exception as e:
            logger.error(f"Cloudinary upload error for {student_id}: {str(e)}", exc_info=True)
            return None
