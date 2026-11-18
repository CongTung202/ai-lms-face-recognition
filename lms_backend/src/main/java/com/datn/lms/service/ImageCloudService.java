package com.datn.lms.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.Map;

@Service
@Slf4j
public class ImageCloudService {

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
        log.info("ImageCloudService đã sẵn sàng kết nối Cloudinary: {}", cloudName);
    }

    /**
     * Nén ảnh (giảm độ phân giải tối đa 1600px, giảm chất lượng JPEG ~80%)
     * Giúp tiết kiệm băng thông và tối ưu tốc độ nạp trang.
     */
    public byte[] compressImage(byte[] originalBytes, float quality, int maxDimension) {
        if (originalBytes == null || originalBytes.length == 0) {
            return originalBytes;
        }

        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(originalBytes);
            BufferedImage originalImage = ImageIO.read(bais);
            if (originalImage == null) {
                log.warn("Không thể đọc ảnh bằng ImageIO, giữ nguyên dữ liệu gốc.");
                return originalBytes;
            }

            int originWidth = originalImage.getWidth();
            int originHeight = originalImage.getHeight();

            int targetWidth = originWidth;
            int targetHeight = originHeight;

            // Tính toán tỷ lệ co nếu vượt quá kích thước tối đa
            if (originWidth > maxDimension || originHeight > maxDimension) {
                if (originWidth >= originHeight) {
                    targetWidth = maxDimension;
                    targetHeight = (int) Math.round((double) originHeight * maxDimension / originWidth);
                } else {
                    targetHeight = maxDimension;
                    targetWidth = (int) Math.round((double) originWidth * maxDimension / originHeight);
                }
            }

            // Tạo ảnh mới với kích thước tối ưu
            BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = resizedImage.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Vẽ nền trắng nếu ảnh gốc có nền trong suốt
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, targetWidth, targetHeight);
            g2d.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
            g2d.dispose();

            // Nén JPEG với tham số quality
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
            if (!writers.hasNext()) {
                ImageIO.write(resizedImage, "jpg", baos);
                return baos.toByteArray();
            }

            ImageWriter writer = writers.next();
            ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
            writer.setOutput(ios);

            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(quality);
            }

            writer.write(null, new IIOImage(resizedImage, null, null), param);
            writer.dispose();
            ios.close();

            byte[] compressed = baos.toByteArray();
            log.info("Nén ảnh thành công: {} KB -> {} KB (Giảm {:.1f}%)",
                    originalBytes.length / 1024,
                    compressed.length / 1024,
                    (1.0 - (double) compressed.length / originalBytes.length) * 100);
            return compressed;

        } catch (Exception e) {
            log.error("Lỗi khi nén ảnh: {}, sử dụng ảnh gốc", e.getMessage());
            return originalBytes;
        }
    }

    /**
     * Tải ảnh đã nén lên Cloudinary
     */
    public Map<String, Object> uploadCompressedImage(byte[] imageBytes, String originalFilename, String folder) throws IOException {
        // Nén ảnh: max 1600px, quality 0.82
        byte[] compressedBytes = compressImage(imageBytes, 0.82f, 1600);

        String baseName = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(0, originalFilename.lastIndexOf('.'))
                : (originalFilename != null ? originalFilename : "image");
        String cleanName = baseName.replaceAll("[^a-zA-Z0-9_-]", "_");
        String publicId = "ntu_announcement_" + System.currentTimeMillis() + "_" + cleanName;

        String targetFolder = folder != null ? folder : "lms_datn/announcements";

        @SuppressWarnings("unchecked")
        Map<String, Object> uploadResult = cloudinary.uploader().upload(compressedBytes, ObjectUtils.asMap(
                "resource_type", "image",
                "folder", targetFolder,
                "public_id", publicId,
                "quality", "auto",
                "fetch_format", "auto"
        ));

        log.info("Đã upload ảnh nén lên Cloudinary: public_id={}, url={}", publicId, uploadResult.get("secure_url"));
        return uploadResult;
    }

    /**
     * Tải MultipartFile từ web lên Cloudinary sau khi nén
     */
    public Map<String, Object> uploadMultipartImage(MultipartFile file, String folder) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Tệp tải lên rỗng");
        }
        return uploadCompressedImage(file.getBytes(), file.getOriginalFilename(), folder);
    }

    /**
     * Tải tệp File từ ổ đĩa lên Cloudinary sau khi nén
     */
    public Map<String, Object> uploadFileImage(File file, String folder) throws IOException {
        byte[] bytes = Files.readAllBytes(file.toPath());
        return uploadCompressedImage(bytes, file.getName(), folder);
    }
}

