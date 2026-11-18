package com.datn.lms.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.springframework.web.multipart.MultipartFile;

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

    /**
     * Tải ảnh bìa (thumbnail) khóa học lên Cloudinary CDN
     *
     * @param file Tệp ảnh tải lên từ máy tính (MultipartFile)
     * @param folder Thư mục lưu trữ trên Cloudinary (vd: "lms_datn/thumbnails")
     * @return URL CDN an toàn (HTTPS)
     */
    public String uploadImageToCloudinary(MultipartFile file, String folder) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Tệp ảnh tải lên không được để trống");
        }

        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "thumbnail.jpg";
        String baseName = originalName.contains(".") ? originalName.substring(0, originalName.lastIndexOf('.')) : originalName;
        String cleanName = baseName.replaceAll("[^a-zA-Z0-9_-]", "_");
        String publicId = "course_thumb_" + System.currentTimeMillis() + "_" + cleanName;

        log.info("Bắt đầu tải ảnh bìa lên Cloudinary (File: {}, Dung lượng: {} KB)...",
                originalName, file.getSize() / 1024);

        @SuppressWarnings("unchecked")
        Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "resource_type", "image",
                "folder", folder != null ? folder : "lms_datn/thumbnails",
                "public_id", publicId
        ));

        String secureUrl = (String) uploadResult.get("secure_url");
        log.info("Upload ảnh bìa lên Cloudinary thành công! URL: {}", secureUrl);
        return secureUrl;
    }
}
