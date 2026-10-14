package com.datn.lms.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Map;

@Service
@Slf4j
public class CloudinaryVideoService {

    @Value("${cloudinary.cloud-name:dhefmthim}")
    private String cloudName;

    @Value("${cloudinary.api-key:253544761519318}")
    private String apiKey;

    @Value("${cloudinary.api-secret:IEoeKaJDt1t-P3i31TeOMzF4rKw}")
    private String apiSecret;

    private Cloudinary cloudinary;

    @PostConstruct
    public void init() {
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
        log.info("Cloudinary Video Service đã sẵn sàng kết nối: {}", cloudName);
    }

    /**
     * Tải tệp video lên Cloudinary theo phương thức Chunked Upload (uploadLarge)
     *
     * @param videoFile Tệp video (đã được nén tối ưu)
     * @param lessonId ID bài học để phân nhóm thư mục
     * @return URL CDN video trực tuyến an toàn (HTTPS)
     */
    public String uploadVideoToCloudinary(File videoFile, Long lessonId) throws IOException {
        log.info("Bắt đầu tải video lên Cloudinary (File: {}, Dung lượng: {} MB)...",
                videoFile.getName(), videoFile.length() / (1024 * 1024));

        String publicId = "lesson_" + lessonId + "_" + System.currentTimeMillis();

        @SuppressWarnings("unchecked")
        Map<String, Object> uploadResult = cloudinary.uploader().uploadLarge(videoFile, ObjectUtils.asMap(
                "resource_type", "video",
                "folder", "lms_datn/videos",
                "public_id", publicId,
                "chunk_size", 20 * 1024 * 1024 // Chia khối 20MB chống timeout mạng
        ));

        String secureUrl = (String) uploadResult.get("secure_url");
        log.info("Upload Cloudinary thành công! URL: {}", secureUrl);
        return secureUrl;
    }
}
