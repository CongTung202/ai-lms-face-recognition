package com.datn.lms.service;

import com.datn.lms.entity.mysql.Lesson;
import com.datn.lms.entity.mysql.VideoAsset;
import com.datn.lms.repository.mysql.LessonRepository;
import com.datn.lms.repository.mysql.VideoAssetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoStreamingService {

    private final LessonRepository lessonRepository;
    private final VideoAssetRepository videoAssetRepository;
    private final VideoCompressorService videoCompressorService;
    private final CloudinaryVideoService cloudinaryVideoService;

    @Value("${storage.video-dir:d:/DATN/uploads/videos}")
    private String videoStorageDir;

    private static final long CHUNK_SIZE = 1024 * 1024; // 1MB per chunk

    /**
     * Tải lên video bài giảng: Tự động nén HD 720p & Đẩy lên Cloudinary CDN
     */
    @Transactional
    public VideoAsset uploadLessonVideo(Long lessonId, MultipartFile file) throws IOException {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài học id=" + lessonId));

        // 1. Tạo thư mục tạm thời trên ổ đĩa để xử lý nén
        Path lessonDir = Paths.get(videoStorageDir, "lesson_" + lessonId);
        Files.createDirectories(lessonDir);

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "video.mp4";
        String cleanOriginalName = "temp_raw_" + System.currentTimeMillis() + "_" + originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
        Path rawDestination = lessonDir.resolve(cleanOriginalName);

        File compressedFile = null;
        try {
            // 2. Lưu file gốc tạm thời vào máy chủ để nén/tải lên
            Files.copy(file.getInputStream(), rawDestination, StandardCopyOption.REPLACE_EXISTING);
            log.info("Lưu tạm video để xử lý nén/upload cho bài học {}: {} ({} MB)",
                    lessonId, rawDestination, rawDestination.toFile().length() / (1024 * 1024));

            // 3. Tiến hành Nén Video về chuẩn Web HD 720p (H.264, bitrate 1.5Mbps)
            compressedFile = lessonDir.resolve("temp_compressed_" + System.currentTimeMillis() + ".mp4").toFile();
            int durationSeconds = videoCompressorService.compressVideoForWeb(rawDestination.toFile(), compressedFile);

            // Chọn file để upload (nếu nén thành công thì lấy file nén, ngược lại lấy file gốc)
            File fileToUpload = (compressedFile.exists() && compressedFile.length() > 0) ? compressedFile : rawDestination.toFile();

            // 4. Đẩy video lên Cloudinary CDN
            String cloudinaryUrl = null;
            VideoAsset.StorageProvider provider = VideoAsset.StorageProvider.CLOUDINARY;
            try {
                cloudinaryUrl = cloudinaryVideoService.uploadVideoToCloudinary(fileToUpload, lessonId);
                log.info("Video bài học {} đã tải lên Cloudinary thành công: {}", lessonId, cloudinaryUrl);
            } catch (Exception ex) {
                log.error("Lỗi tải lên Cloudinary cho bài học {}: {}", lessonId, ex.getMessage(), ex);
                throw new IOException("Không thể đẩy video lên Cloudinary: " + ex.getMessage(), ex);
            }

            // 5. Cập nhật hoặc tạo mới VideoAsset trong MySQL
            VideoAsset videoAsset = videoAssetRepository.findByLessonId(lessonId)
                    .orElse(VideoAsset.builder()
                            .lesson(lesson)
                            .storageProvider(provider)
                            .encodeStatus(VideoAsset.EncodeStatus.READY)
                            .build());

            videoAsset.setOriginalFileName(originalFilename);
            videoAsset.setStorageProvider(provider);
            videoAsset.setDurationSeconds(durationSeconds > 0 ? durationSeconds : lesson.getDurationSeconds());
            videoAsset.setFileSizeBytes(fileToUpload.length());
            videoAsset.setResolution("720p HD");
            videoAsset.setRawVideoUrl(cloudinaryUrl);
            videoAsset.setHlsPlaylistUrl(cloudinaryUrl);

            VideoAsset savedAsset = videoAssetRepository.save(videoAsset);

            // 6. XÓA SẠCH HOÀN TOÀN TẤT CẢ FILE VIDEO Ở LOCAL TRONG THƯ MỤC GỐC
            deleteLocalLessonFiles(lessonId);
            log.info("Đã xóa sạch toàn bộ video trong thư mục gốc trên máy local cho bài học {}. Chỉ phát từ Cloudinary CDN.", lessonId);

            return savedAsset;
        } finally {
            // Dọn dẹp an toàn các file tạm còn sót nếu có ngoại lệ
            try {
                if (Files.exists(rawDestination)) Files.deleteIfExists(rawDestination);
                if (compressedFile != null && compressedFile.exists()) compressedFile.delete();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Dọn dẹp và xóa hoàn toàn thư mục video cục bộ của bài học
     */
    public void deleteLocalLessonFiles(Long lessonId) {
        if (lessonId == null) return;
        try {
            Path lessonDir = Paths.get(videoStorageDir, "lesson_" + lessonId);
            if (Files.exists(lessonDir)) {
                try (var stream = Files.walk(lessonDir)) {
                    stream.sorted(java.util.Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(File::delete);
                }
                log.info("Đã dọn dẹp sạch thư mục cục bộ bài học {}: {}", lessonId, lessonDir);
            }
        } catch (Exception e) {
            log.warn("Không thể dọn dẹp thư mục cục bộ bài học {}: {}", lessonId, e.getMessage());
        }
    }

    /**
     * Phát video nhanh chóng:
     * - Nếu video trên Cloudinary: Redirect 302 sang CDN Cloudinary phát trực tiếp
     * - Nếu video ở Local: Phát HTTP 206 Partial Content (Byte-Range Request)
     */
    public ResponseEntity<byte[]> streamVideoChunk(Long lessonId, String rangeHeader) {
        VideoAsset videoAsset = videoAssetRepository.findByLessonId(lessonId)
                .orElseThrow(() -> new RuntimeException("Bài học này chưa có video"));

        // 1. Nếu video đã được lưu trên Cloudinary CDN, chuyển hướng phát trực tiếp từ CDN
        String videoUrl = videoAsset.getRawVideoUrl();
        if (videoUrl == null || !videoUrl.startsWith("http")) {
            videoUrl = videoAsset.getHlsPlaylistUrl();
        }
        if (videoUrl != null && videoUrl.startsWith("http")) {
            HttpHeaders redirectHeaders = new HttpHeaders();
            redirectHeaders.set(HttpHeaders.LOCATION, videoUrl);
            return new ResponseEntity<>(redirectHeaders, HttpStatus.FOUND);
        }

        // 2. Tìm file video trong thư mục lưu trữ cục bộ của bài học (nếu có)
        Path lessonDir = Paths.get(videoStorageDir, "lesson_" + lessonId);
        if (!Files.exists(lessonDir)) {
            throw new RuntimeException("Không tìm thấy tệp video trên máy chủ hoặc Cloud");
        }

        File[] files = lessonDir.toFile().listFiles((dir, name) -> name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".webm"));
        if (files == null || files.length == 0) {
            throw new RuntimeException("Tệp video không tồn tại trên ổ đĩa");
        }

        File videoFile = files[0];
        long fileSize = videoFile.length();

        // 1. Nếu trình duyệt không yêu cầu Range, phát cả file hoặc 1 chunk đầu tiên
        if (rangeHeader == null || !rangeHeader.startsWith("bytes=")) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("video/mp4"));
            headers.setContentLength(fileSize);
            headers.set("Accept-Ranges", "bytes");

            try {
                byte[] data = Files.readAllBytes(videoFile.toPath());
                return new ResponseEntity<>(data, headers, HttpStatus.OK);
            } catch (IOException e) {
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        // 2. Xử lý HTTP Byte Range: bytes=start-end
        try {
            String[] ranges = rangeHeader.replace("bytes=", "").split("-");
            long rangeStart = Long.parseLong(ranges[0]);
            long rangeEnd = rangeStart + CHUNK_SIZE - 1;

            if (ranges.length > 1 && !ranges[1].isEmpty()) {
                rangeEnd = Long.parseLong(ranges[1]);
            }

            if (rangeEnd >= fileSize) {
                rangeEnd = fileSize - 1;
            }

            long contentLength = rangeEnd - rangeStart + 1;
            byte[] data = new byte[(int) contentLength];

            try (RandomAccessFile raf = new RandomAccessFile(videoFile, "r")) {
                raf.seek(rangeStart);
                raf.readFully(data);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("video/mp4"));
            headers.set("Accept-Ranges", "bytes");
            headers.set("Content-Range", String.format("bytes %d-%d/%d", rangeStart, rangeEnd, fileSize));
            headers.setContentLength(contentLength);

            return new ResponseEntity<>(data, headers, HttpStatus.PARTIAL_CONTENT);

        } catch (Exception e) {
            log.error("Lỗi khi stream video chunk: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
        }
    }
}
