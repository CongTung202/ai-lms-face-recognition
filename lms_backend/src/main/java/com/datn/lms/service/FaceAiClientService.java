package com.datn.lms.service;

import com.datn.lms.dto.FaceAiEnrollResultDto;
import com.datn.lms.dto.FaceAiVerifyResultDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@Slf4j
public class FaceAiClientService {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String enrollEndpoint;
    private final String verifyEndpoint;

    public FaceAiClientService(
            @Value("${ai-service.face-api.base-url:http://localhost:8001}") String baseUrl,
            @Value("${ai-service.face-api.enroll-endpoint:/v1/face/enroll-multi-angle}") String enrollEndpoint,
            @Value("${ai-service.face-api.verify-endpoint:/v1/face/verify}") String verifyEndpoint) {
        this.restTemplate = new RestTemplate();
        this.baseUrl = baseUrl;
        this.enrollEndpoint = enrollEndpoint;
        this.verifyEndpoint = verifyEndpoint;
    }

    /**
     * Gửi 3 góc mặt (straight, left, right) sang Python FastAPI để đăng ký vector và tải ảnh lên Cloudinary
     */
    public FaceAiEnrollResultDto enrollFaceMultiAngle(
            String studentCode,
            String fullName,
            String department,
            MultipartFile imageStraight,
            MultipartFile imageLeft,
            MultipartFile imageRight) throws IOException {

        String url = baseUrl + enrollEndpoint;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("employee_id", studentCode);
        body.add("full_name", fullName);
        body.add("department", department);

        if (imageStraight != null && !imageStraight.isEmpty()) {
            body.add("image_straight", toResource(imageStraight, "straight.jpg"));
        }
        if (imageLeft != null && !imageLeft.isEmpty()) {
            body.add("image_left", toResource(imageLeft, "left.jpg"));
        }
        if (imageRight != null && !imageRight.isEmpty()) {
            body.add("image_right", toResource(imageRight, "right.jpg"));
        }

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            log.info("Đang gọi Face AI Enroll: {} cho MSSV: {}", url, studentCode);
            ResponseEntity<FaceAiEnrollResultDto> response = restTemplate.postForEntity(url, requestEntity, FaceAiEnrollResultDto.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Lỗi khi gọi Face AI enroll API: {}", e.getMessage());
            throw new RuntimeException("Không thể kết nối dịch vụ Face AI: " + e.getMessage(), e);
        }
    }

    /**
     * Gửi 1 ảnh webcam sang Python FastAPI để đối chiếu 1:1 xác thực khuôn mặt sinh viên
     */
    public FaceAiVerifyResultDto verifyFace(String studentCode, MultipartFile image) throws IOException {
        String url = baseUrl + verifyEndpoint + "?threshold=0.65&check_liveness=true&check_quality=true";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("employee_id", studentCode);
        body.add("image", toResource(image, "capture.jpg"));

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            log.info("Đang gọi Face AI Verify: {} cho MSSV: {}", url, studentCode);
            ResponseEntity<FaceAiVerifyResultDto> response = restTemplate.postForEntity(url, requestEntity, FaceAiVerifyResultDto.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Lỗi khi gọi Face AI verify API: {}", e.getMessage());
            throw new RuntimeException("Không thể kết nối dịch vụ Face AI: " + e.getMessage(), e);
        }
    }

    /**
     * Chuyển MultipartFile thành ByteArrayResource có kèm filename để RestTemplate nhận dạng đúng Part
     */
    private ByteArrayResource toResource(MultipartFile file, String defaultFilename) throws IOException {
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : defaultFilename;
        return new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
    }
}
