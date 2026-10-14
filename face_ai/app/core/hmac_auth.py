import time
import hmac
import hashlib
from fastapi import Request, HTTPException, status
from fastapi.responses import JSONResponse
from starlette.middleware.base import BaseHTTPMiddleware
from app.config import settings

def compute_hmac_signature(secret: str, timestamp_str: str) -> str:
    """Computes HMAC-SHA256 signature for internal microservice communication"""
    return hmac.new(
        secret.encode("utf-8"),
        timestamp_str.encode("utf-8"),
        hashlib.sha256
    ).hexdigest()

class HmacInternalAuthMiddleware(BaseHTTPMiddleware):
    """
    Middleware xác thực chữ ký HMAC giữa Node.js Backend và Python AI Microservice (Pha 3).
    - Ngăn chặn triệt để các kết nối trái phép gọi trực tiếp vào Python AI mà không qua Node.js.
    - Chống tấn công Replay Attack bằng cách giới hạn độ lệch timestamp tối đa 60 giây.
    """
    async def dispatch(self, request: Request, call_next):
        # Bỏ qua xác thực cho các route công khai (Health check, Swagger Docs, Web UI, WebSocket stream)
        public_paths = ["/", "/health", "/docs", "/openapi.json", "/redoc"]
        path = request.url.path

        if path in public_paths or path.startswith("/static") or path.startswith("/ws") or "/ws/" in path or not settings.REQUIRE_HMAC_AUTH:
            return await call_next(request)

        # Chỉ áp dụng HMAC cho các API nhận diện / dữ liệu (/v1/*, /api/*)
        if not (path.startswith("/v1/") or path.startswith("/api/")):
            return await call_next(request)

        # Lấy Header xác thực
        timestamp_header = request.headers.get("x-internal-timestamp") or request.headers.get("X-Internal-Timestamp")
        signature_header = request.headers.get("x-internal-signature") or request.headers.get("X-Internal-Signature")

        if not timestamp_header or not signature_header:
            return JSONResponse(
                status_code=status.HTTP_401_UNAUTHORIZED,
                content={
                    "success": False,
                    "code": "HMAC_AUTH_REQUIRED",
                    "detail": "Truy cập bị từ chối: Yêu cầu xác thực chữ ký bảo mật nội bộ (X-Internal-Signature & X-Internal-Timestamp)!"
                }
            )

        # Kiểm tra Timestamp chống Replay Attack (sai lệch không quá 60 giây)
        try:
            req_time = int(timestamp_header)
            current_time = int(time.time())
            if abs(current_time - req_time) > 60:
                return JSONResponse(
                    status_code=status.HTTP_403_FORBIDDEN,
                    content={
                        "success": False,
                        "code": "HMAC_TIMESTAMP_EXPIRED",
                        "detail": f"Chữ ký đã hết hạn (Timestamp lệch {abs(current_time - req_time)}s > 60s). Có nguy cơ bị Replay Attack!"
                    }
                )
        except ValueError:
            return JSONResponse(
                status_code=status.HTTP_400_BAD_REQUEST,
                content={
                    "success": False,
                    "code": "INVALID_TIMESTAMP_FORMAT",
                    "detail": "Định dạng timestamp không hợp lệ."
                }
            )

        # Tính toán chữ ký HMAC mong đợi
        expected_sig = compute_hmac_signature(settings.INTERNAL_AI_SECRET, timestamp_header)

        # So khớp chữ ký an toàn tránh Timing Attack
        if not hmac.compare_digest(expected_sig.lower(), signature_header.lower()):
            return JSONResponse(
                status_code=status.HTTP_403_FORBIDDEN,
                content={
                    "success": False,
                    "code": "HMAC_SIGNATURE_MISMATCH",
                    "detail": "Chữ ký bảo mật HMAC không chính xác! Yêu cầu truy cập từ nguồn không đáng tin cậy."
                }
            )

        return await call_next(request)
