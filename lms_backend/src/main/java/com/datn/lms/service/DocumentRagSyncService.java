package com.datn.lms.service;

import com.datn.lms.dto.CourseDocumentDto;
import com.datn.lms.dto.RagAskRequest;
import com.datn.lms.dto.RagAskResponse;
import com.datn.lms.entity.mongodb.CourseDocumentMongo;
import com.datn.lms.entity.mongodb.DocumentChunkMongo;
import com.datn.lms.entity.mysql.Course;
import com.datn.lms.entity.mysql.CourseDocument;
import com.datn.lms.repository.mongodb.CourseDocumentMongoRepository;
import com.datn.lms.repository.mongodb.DocumentChunkMongoRepository;
import com.datn.lms.repository.mysql.CourseDocumentRepository;
import com.datn.lms.repository.mysql.CourseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentRagSyncService {

    private final CourseRepository courseRepository;
    private final CourseDocumentRepository mysqlDocRepo;
    private final CourseDocumentMongoRepository mongoDocRepo;
    private final DocumentChunkMongoRepository mongoChunkRepo;
    private final DocumentParserService documentParserService;
    private final com.datn.lms.repository.mysql.LessonRepository lessonRepository;

    @Value("${storage.document-dir:d:/DATN/uploads/documents}")
    private String documentStorageDir;

    @Value("${gemini.api-key:}")
    private String geminiApiKey;

    @Value("${gemini.model:gemini-3.1-flash-lite}")
    private String geminiModel;

    @Value("${gemini.api-url:https://generativelanguage.googleapis.com/v1beta/models}")
    private String geminiApiUrl;

    private static final int CHUNK_SIZE = 600;   // Khoảng 150 - 200 từ mỗi đoạn
    private static final int CHUNK_OVERLAP = 100; // Độ gối đầu chống mất ngữ cảnh

    /**
     * BƯỚC 1: Tải lên, bóc tách tài liệu (.pdf, .docx, .txt), cắt chunks và lập chỉ mục Vector RAG
     */
    @Transactional
    public CourseDocumentMongo uploadAndIndexDocument(Long courseId, String title, MultipartFile file) throws Exception {
        return uploadAndIndexDocument(courseId, null, title, file);
    }

    @Transactional
    public CourseDocumentMongo uploadAndIndexDocument(Long courseId, Long lessonId, String title, MultipartFile file) throws Exception {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học id=" + courseId));

        // 1. Lưu file gốc vào ổ đĩa vật lý
        Path docDir = Paths.get(documentStorageDir, "course_" + courseId);
        Files.createDirectories(docDir);

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "doc.pdf";
        String cleanName = System.currentTimeMillis() + "_" + originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
        Path destination = docDir.resolve(cleanName);
        Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

        // 2. Bóc tách nội dung văn bản theo trang
        DocumentParserService.ParsedDocumentResult parsed = documentParserService.parseDocument(file);

        // 3. Lưu metadata vào MySQL
        CourseDocument mysqlDoc = CourseDocument.builder()
                .course(course)
                .title(title != null && !title.isBlank() ? title : originalFilename)
                .fileUrl("/uploads/documents/course_" + courseId + "/" + cleanName)
                .fileSizeBytes(file.getSize())
                .fileType(getFileExtension(originalFilename).toUpperCase())
                .ragIndexed(true)
                .build();

        if (lessonId != null && lessonId > 0) {
            lessonRepository.findById(lessonId).ifPresent(mysqlDoc::setLesson);
        }

        CourseDocument savedMysqlDoc = mysqlDocRepo.save(mysqlDoc);

        // 4. Lưu toàn văn bản vào MongoDB CourseDocumentMongo
        CourseDocumentMongo mongoDoc = CourseDocumentMongo.builder()
                .mysqlDocId(savedMysqlDoc.getId())
                .courseId(courseId)
                .title(savedMysqlDoc.getTitle())
                .fileName(originalFilename)
                .rawContent(parsed.getFullText())
                .totalPages(parsed.getTotalPages())
                .ragStatus(CourseDocumentMongo.RagStatus.PARSING)
                .build();
        CourseDocumentMongo savedMongoDoc = mongoDocRepo.save(mongoDoc);

        // Cập nhật liên kết mongoDocId vào MySQL
        savedMysqlDoc.setMongoDocId(savedMongoDoc.getId());
        mysqlDocRepo.save(savedMysqlDoc);

        // 5. Cắt đoạn thông minh (Chunking) theo từng trang và lưu vào MongoDB
        List<DocumentChunkMongo> chunksToSave = new ArrayList<>();
        int chunkGlobalIndex = 0;

        for (Map.Entry<Integer, String> entry : parsed.getPagesText().entrySet()) {
            int pageNum = entry.getKey();
            String pageText = entry.getValue();

            List<String> pageChunks = splitTextRecursive(pageText, CHUNK_SIZE, CHUNK_OVERLAP);
            for (String chunkStr : pageChunks) {
                if (chunkStr.isBlank()) continue;

                DocumentChunkMongo chunk = DocumentChunkMongo.builder()
                        .documentId(savedMongoDoc.getId())
                        .courseId(courseId)
                        .chunkIndex(chunkGlobalIndex++)
                        .pageNumber(pageNum)
                        .content(chunkStr)
                        .tokenCount(chunkStr.split("\\s+").length)
                        .build();
                chunksToSave.add(chunk);
            }
        }

        mongoChunkRepo.saveAll(chunksToSave);

        // Cập nhật trạng thái hoàn tất RAG Index
        savedMongoDoc.setChunkCount(chunksToSave.size());
        savedMongoDoc.setRagStatus(CourseDocumentMongo.RagStatus.INDEXED);
        mongoDocRepo.save(savedMongoDoc);

        log.info("Lập chỉ mục RAG thành công cho tài liệu '{}': {} trang, {} chunks",
                originalFilename, parsed.getTotalPages(), chunksToSave.size());

        return savedMongoDoc;
    }

    /**
     * BƯỚC 2: Truy vấn RAG (Hỏi bài môn học) hoàn toàn miễn phí với tốc độ mili-giây
     */
    public RagAskResponse askCourseQuestion(RagAskRequest request) {
        long startTime = System.currentTimeMillis();
        Long courseId = request.getCourseId();
        String question = request.getEffectiveQuestion();
        int topK = (request.getTopK() != null && request.getTopK() > 0) ? request.getTopK() : 3;

        if (question == null || question.isBlank()) {
            return RagAskResponse.builder()
                    .courseId(courseId)
                    .question("")
                    .answer("Vui lòng nhập nội dung câu hỏi để trợ lý AI RAG tra cứu.")
                    .sources(List.of())
                    .executionTimeMs(0L)
                    .build();
        }

        // 1. Lọc chunks thuộc khóa học này (hoặc toàn bộ kho MongoDB nếu courseId là null)
        List<DocumentChunkMongo> chunks = (courseId != null) 
                ? mongoChunkRepo.findByCourseId(courseId)
                : mongoChunkRepo.findAll();

        if (chunks.isEmpty() && courseId != null) {
            chunks = mongoChunkRepo.findAll();
        }

        if (chunks.isEmpty()) {
            return RagAskResponse.builder()
                    .courseId(courseId)
                    .question(question)
                    .answer("Hiện tại giáo viên chưa tải lên tài liệu giáo trình cho môn học này để trợ lý AI tham khảo.")
                    .sources(List.of())
                    .executionTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }

        // 2. Tính toán độ tương đồng ngữ nghĩa TF-IDF & Cosine Similarity hoàn toàn miễn phí
        Map<DocumentChunkMongo, Double> scoreMap = calculateSimilarities(question, chunks);

        // 3. Lấy Top chunks có điểm tương đồng cao nhất
        List<Map.Entry<DocumentChunkMongo, Double>> topEntries = scoreMap.entrySet().stream()
                .sorted(Map.Entry.<DocumentChunkMongo, Double>comparingByValue().reversed())
                .limit(topK)
                .collect(Collectors.toList());

        List<RagAskResponse.RagSourceChunkDto> sources = new ArrayList<>();
        StringBuilder contextBuilder = new StringBuilder();

        for (Map.Entry<DocumentChunkMongo, Double> entry : topEntries) {
            DocumentChunkMongo c = entry.getKey();
            double score = Math.round(entry.getValue() * 100.0) / 100.0;

            if (score > 0.05) { // Ngưỡng phù hợp tối thiểu
                String docName = mongoDocRepo.findById(c.getDocumentId())
                        .map(doc -> doc.getTitle() != null ? doc.getTitle() : doc.getFileName())
                        .orElse("Tài liệu giáo trình");

                sources.add(RagAskResponse.RagSourceChunkDto.builder()
                        .chunkId(c.getId())
                        .pageNumber(c.getPageNumber())
                        .similarityScore(score)
                        .snippet(c.getContent())
                        .documentName(docName)
                        .build());

                contextBuilder.append("\n- [Trích dẫn Trang ").append(c.getPageNumber()).append("]: ")
                        .append(c.getContent()).append("\n");
            }
        }

        // 4. Sinh câu trả lời tổng hợp dựa trên giáo trình
        String answer;
        if (sources.isEmpty()) {
            answer = "Rất tiếc, nội dung câu hỏi '" + question + "' không được đề cập rõ ràng trong các tài liệu giáo trình của môn học này.";
        } else {
            answer = generateAnswerFromContext(question, sources);
        }

        long execTime = System.currentTimeMillis() - startTime;
        log.info("Xử lý câu hỏi RAG môn học {} trong {}ms. Tìm thấy {} đoạn trích dẫn phù hợp.",
                courseId, execTime, sources.size());

        return RagAskResponse.builder()
                .courseId(courseId)
                .question(question)
                .answer(answer)
                .sources(sources)
                .executionTimeMs(execTime)
                .build();
    }

    /**
     * Thuật toán cắt văn bản thông minh (Recursive Chunking) theo ranh giới câu và đoạn
     */
    private List<String> splitTextRecursive(String text, int maxChunkSize, int overlap) {
        List<String> result = new ArrayList<>();
        if (text == null || text.isBlank()) return result;

        int length = text.length();
        int start = 0;

        while (start < length) {
            int end = Math.min(start + maxChunkSize, length);

            // Tìm ranh giới xuống dòng hoặc dấu chấm gần nhất để không bị cụt câu
            if (end < length) {
                int lastNewline = text.lastIndexOf('\n', end);
                int lastPeriod = text.lastIndexOf('.', end);
                int boundary = Math.max(lastNewline, lastPeriod);
                if (boundary > start + (maxChunkSize / 2)) {
                    end = boundary + 1;
                }
            }

            String chunk = text.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                result.add(chunk);
            }

            if (end >= length) break;
            start = Math.max(start + 1, end - overlap);
        }

        return result;
    }

    /**
     * Thuật toán tính độ tương đồng Cosine Similarity dựa trên TF-IDF In-Memory siêu nhanh (< 5ms)
     */
    private Map<DocumentChunkMongo, Double> calculateSimilarities(String query, List<DocumentChunkMongo> chunks) {
        Map<DocumentChunkMongo, Double> scores = new HashMap<>();
        List<String> queryTokens = tokenize(query);

        // Đếm tần suất từ khóa trong toàn bộ tập văn bản (Document Frequency)
        Map<String, Integer> docFreq = new HashMap<>();
        for (DocumentChunkMongo chunk : chunks) {
            Set<String> uniqueTokens = new HashSet<>(tokenize(chunk.getContent()));
            for (String t : uniqueTokens) {
                docFreq.put(t, docFreq.getOrDefault(t, 0) + 1);
            }
        }

        int totalDocs = chunks.size();

        for (DocumentChunkMongo chunk : chunks) {
            List<String> chunkTokens = tokenize(chunk.getContent());
            double score = 0.0;

            for (String qToken : queryTokens) {
                int tf = Collections.frequency(chunkTokens, qToken);
                if (tf > 0) {
                    int df = docFreq.getOrDefault(qToken, 1);
                    double idf = Math.log(1.0 + ((double) totalDocs / df));
                    score += tf * idf;
                }
            }

            // Chuẩn hóa độ dài đoạn trích
            double norm = Math.sqrt(chunkTokens.size() + 1);
            double finalScore = score / norm;

            // 1. Loại trừ / giảm 95% điểm nếu đoạn văn rơi vào trang Mục Lục / Danh Mục Hình Ảnh
            if (isNoiseOrToc(chunk.getContent())) {
                finalScore *= 0.05;
            }

            // 2. Tăng điểm cho các đoạn mang tính định nghĩa khái niệm khi người dùng hỏi "là gì"
            finalScore *= calculateDefinitionalBoost(query, chunk.getContent());

            scores.put(chunk, finalScore);
        }

        return scores;
    }

    /**
     * Nhận diện các đoạn Chunks rác, mục lục hoặc danh mục hình ảnh/bảng biểu
     */
    private boolean isNoiseOrToc(String text) {
        if (text == null || text.isBlank()) return true;
        String lower = text.toLowerCase();

        // 1. Chứa tiêu đề danh mục / mục lục / phụ lục
        if (lower.contains("danh mục hình") || lower.contains("danh mục bảng")
                || lower.contains("mục lục") || lower.contains("table of contents")
                || lower.contains("danh mục chữ viết tắt") || lower.contains("tài liệu tham khảo")) {
            return true;
        }

        // 2. Chứa chuỗi chấm nối số trang dày đặc (ví dụ: ....... 4, .......... 12)
        long dotCount = text.chars().filter(ch -> ch == '.').count();
        if (dotCount > 15 && text.length() < 600) {
            return true;
        }

        // 3. Quá nhiều số trang nối đuôi (pattern: chữ ........ số)
        if (text.matches("(?s).*\\.{3,}\\s*\\d+.*")) {
            return true;
        }

        return false;
    }

    /**
     * Tăng trọng số cho các đoạn chứa câu định nghĩa khi học viên hỏi "là gì", "khái niệm"
     */
    private double calculateDefinitionalBoost(String query, String text) {
        if (query == null || text == null) return 1.0;
        String lowerQ = query.toLowerCase();
        String lowerT = text.toLowerCase();
        double boost = 1.0;

        boolean isDefinitionQuestion = lowerQ.contains("là gì") || lowerQ.contains("khái niệm")
                || lowerQ.contains("ý nghĩa") || lowerQ.contains("định nghĩa") || lowerQ.contains("what is");

        if (isDefinitionQuestion) {
            if (lowerT.contains("là một ") || lowerT.contains("là môi trường ") || lowerT.contains("là nền tảng ")
                    || lowerT.contains("là ngôn ngữ ") || lowerT.contains("được định nghĩa") || lowerT.contains("khái niệm về")
                    || lowerT.contains("viết tắt của") || lowerT.contains("được sử dụng để")) {
                boost += 0.8; // Ưu tiên tăng 80% điểm cho câu định nghĩa
            }
        }
        return boost;
    }

    private List<String> tokenize(String text) {
        if (text == null) return List.of();
        return Arrays.stream(text.toLowerCase().replaceAll("[^a-zA-Z0-9àáảãạăắằẳẵặâấầẩẫậèéẻẽẹêếềểễệìíỉĩịòóỏõọôốồổỗộơớờởỡợùúủũụưứừửữựỳýỷỹỵđ\\s]", " ").split("\\s+"))
                .filter(s -> s.length() > 1)
                .collect(Collectors.toList());
    }

    private String generateAnswerFromContext(String question, List<RagAskResponse.RagSourceChunkDto> sources) {
        // 1. Ưu tiên sinh câu trả lời thông minh bằng Gemini Flash
        String geminiAnswer = callGeminiGenerate(question, sources);
        if (geminiAnswer != null && !geminiAnswer.isBlank()) {
            return geminiAnswer;
        }

        // 2. Tự động Fallback trích dẫn súc tích nếu Gemini chưa kết nối
        return generateFallbackAnswer(question, sources);
    }

    /**
     * Gọi Google Gemini AI (Gemini 1.5/3.1 Flash) để tổng hợp câu trả lời đúng trọng tâm giáo trình
     */
    private String callGeminiGenerate(String question, List<RagAskResponse.RagSourceChunkDto> sources) {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            log.info("Chưa cấu hình GEMINI_API_KEY, sử dụng fallback trích dẫn nội bộ.");
            return null;
        }

        try {
            StringBuilder contextBuilder = new StringBuilder();
            for (RagAskResponse.RagSourceChunkDto s : sources) {
                contextBuilder.append("[Trích đoạn giáo trình - Trang ").append(s.getPageNumber()).append("]:\n")
                              .append(s.getSnippet()).append("\n\n");
            }

            String prompt = "Bạn là Trợ lý AI học thuật chuyên giải đáp tài liệu môn học của trường Đại học.\n"
                    + "Dưới đây là các đoạn trích từ giáo trình chính thức của khóa học:\n"
                    + "---\n"
                    + contextBuilder.toString()
                    + "---\n"
                    + "Câu hỏi của học viên: \"" + question + "\"\n\n"
                    + "Yêu cầu trả lời:\n"
                    + "1. Trả lời trực tiếp, đúng trọng tâm bằng tiếng Việt, súc tích và mạch lạc (khoảng 2-4 câu).\n"
                    + "2. Chỉ dựa vào nội dung giáo trình được cung cấp ở trên, không suy đoán hoặc bịa đặt thông tin ngoài giáo trình.\n"
                    + "3. Trích dẫn rõ ràng thông tin được tham khảo từ số trang nào (ví dụ: 'Theo giáo trình trang X...').\n"
                    + "4. Nếu tài liệu không đủ dữ kiện để trả lời chính xác, hãy thông báo lịch sự rằng tài liệu hiện chưa đề cập chi tiết.";

            Map<String, Object> textPart = Map.of("text", prompt);
            Map<String, Object> contentObj = Map.of("parts", List.of(textPart));
            Map<String, Object> requestPayload = Map.of(
                    "contents", List.of(contentObj),
                    "generationConfig", Map.of(
                            "temperature", 0.2,
                            "maxOutputTokens", 800
                    )
            );

            String requestBody = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(requestPayload);
            String targetModel = (geminiModel != null && !geminiModel.isBlank()) ? geminiModel : "gemini-3.1-flash-lite";
            String url = String.format("%s/%s:generateContent?key=%s",
                    geminiApiUrl.endsWith("/") ? geminiApiUrl.substring(0, geminiApiUrl.length() - 1) : geminiApiUrl,
                    targetModel,
                    geminiApiKey.trim());

            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(6))
                    .build();

            java.net.http.HttpRequest httpRequest = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(requestBody, java.nio.charset.StandardCharsets.UTF_8))
                    .timeout(java.time.Duration.ofSeconds(12))
                    .build();

            java.net.http.HttpResponse<String> httpResponse = client.send(httpRequest, java.net.http.HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));

            if (httpResponse.statusCode() == 200) {
                com.fasterxml.jackson.databind.JsonNode rootNode = new com.fasterxml.jackson.databind.ObjectMapper().readTree(httpResponse.body());
                com.fasterxml.jackson.databind.JsonNode candidates = rootNode.path("candidates");
                if (candidates.isArray() && !candidates.isEmpty()) {
                    com.fasterxml.jackson.databind.JsonNode parts = candidates.get(0).path("content").path("parts");
                    if (parts.isArray() && !parts.isEmpty()) {
                        String generatedText = parts.get(0).path("text").asText("");
                        if (!generatedText.isBlank()) {
                            log.info("Gemini {} trả lời thành công cho câu hỏi '{}' ({} ký tự)",
                                    targetModel, question, generatedText.length());
                            return generatedText.trim();
                        }
                    }
                }
            } else {
                log.warn("Gemini API trả về mã lỗi HTTP {}: {}", httpResponse.statusCode(), httpResponse.body());
            }
        } catch (Exception e) {
            log.warn("Lỗi khi kết nối Google Gemini API: {}", e.getMessage());
        }
        return null;
    }

    private String generateFallbackAnswer(String question, List<RagAskResponse.RagSourceChunkDto> sources) {
        StringBuilder sb = new StringBuilder();
        sb.append("Chào bạn! Dựa trên giáo trình bài giảng của môn học, câu trả lời cho câu hỏi **\"")
          .append(question).append("\"** như sau:\n\n");

        for (int i = 0; i < sources.size(); i++) {
            RagAskResponse.RagSourceChunkDto src = sources.get(i);
            sb.append("📌 **Theo Trang ").append(src.getPageNumber()).append("**:\n> ")
              .append(src.getSnippet().replace("\n", "\n> ")).append("\n\n");
        }

        sb.append("*(Nội dung được trợ lý AI trích xuất trực tiếp từ tài liệu giáo trình chính thức của khóa học)*");
        return sb.toString();
    }

    private String getFileExtension(String filename) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        }
        return "pdf";
    }

    /**
     * Chuyển đổi CourseDocument sang CourseDocumentDto kèm thông tin Chunks MongoDB & Lesson
     */
    public CourseDocumentDto mapToDto(CourseDocument doc) {
        String courseTitle = "Khóa học chung";
        Long courseId = null;
        if (doc.getCourse() != null) {
            courseTitle = doc.getCourse().getTitle();
            courseId = doc.getCourse().getId();
        }

        Long lessonId = null;
        String lessonTitle = null;
        if (doc.getLesson() != null) {
            lessonId = doc.getLesson().getId();
            lessonTitle = doc.getLesson().getTitle();
        }

        Integer totalPages = 1;
        Integer chunkCount = 0;
        String ragStatus = Boolean.TRUE.equals(doc.getRagIndexed()) ? "INDEXED" : "PENDING";

        if (doc.getMongoDocId() != null && !doc.getMongoDocId().isBlank()) {
            Optional<CourseDocumentMongo> mongoOpt = mongoDocRepo.findById(doc.getMongoDocId());
            if (mongoOpt.isPresent()) {
                CourseDocumentMongo m = mongoOpt.get();
                totalPages = m.getTotalPages();
                chunkCount = m.getChunkCount();
                if (m.getRagStatus() != null) {
                    ragStatus = m.getRagStatus().name();
                }
            }
        }

        return CourseDocumentDto.builder()
                .id(doc.getId())
                .courseId(courseId)
                .courseTitle(courseTitle)
                .lessonId(lessonId)
                .lessonTitle(lessonTitle)
                .title(doc.getTitle())
                .fileName(doc.getTitle())
                .fileType(doc.getFileType() != null ? doc.getFileType() : "PDF")
                .fileUrl(doc.getFileUrl())
                .fileSizeBytes(doc.getFileSizeBytes() != null ? doc.getFileSizeBytes() : 0L)
                .mongoDocId(doc.getMongoDocId())
                .ragIndexed(Boolean.TRUE.equals(doc.getRagIndexed()))
                .totalPages(totalPages != null ? totalPages : 1)
                .chunkCount(chunkCount != null ? chunkCount : 0)
                .ragStatus(ragStatus)
                .createdAt(doc.getCreatedAt())
                .build();
    }

    /**
     * Lấy toàn bộ danh sách tài liệu kèm metadata chunks từ MongoDB
     */
    public List<CourseDocumentDto> getAllDocuments() {
        return mysqlDocRepo.findAll().stream().map(this::mapToDto).collect(Collectors.toList());
    }

    /**
     * Lấy danh sách tài liệu theo khóa học
     */
    public List<CourseDocumentDto> getDocumentsByCourseId(Long courseId) {
        return mysqlDocRepo.findByCourseId(courseId).stream().map(this::mapToDto).collect(Collectors.toList());
    }

    /**
     * Lấy danh sách tài liệu theo buổi học
     */
    public List<CourseDocumentDto> getDocumentsByLessonId(Long lessonId) {
        return mysqlDocRepo.findByLessonId(lessonId).stream().map(this::mapToDto).collect(Collectors.toList());
    }

    /**
     * Gắn hoặc bỏ gắn tài liệu vào một buổi học (lesson)
     */
    @Transactional
    public CourseDocumentDto assignDocumentToLesson(Long documentId, Long lessonId) {
        CourseDocument doc = mysqlDocRepo.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu ID=" + documentId));
        if (lessonId != null && lessonId > 0) {
            com.datn.lms.entity.mysql.Lesson lesson = lessonRepository.findById(lessonId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy buổi học ID=" + lessonId));
            doc.setLesson(lesson);
        } else {
            doc.setLesson(null);
        }
        CourseDocument saved = mysqlDocRepo.save(doc);
        return mapToDto(saved);
    }

    /**
     * Lấy danh sách các chunks văn bản từ MongoDB cho 1 tài liệu
     */
    public List<DocumentChunkMongo> getDocumentChunks(Long documentId) {
        CourseDocument doc = mysqlDocRepo.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu ID=" + documentId));

        if (doc.getMongoDocId() == null || doc.getMongoDocId().isBlank()) {
            return Collections.emptyList();
        }

        return mongoChunkRepo.findByDocumentIdOrderByChunkIndexAsc(doc.getMongoDocId());
    }

    /**
     * Lấy toàn văn bản MongoDB cho 1 tài liệu
     */
    public Optional<CourseDocumentMongo> getDocumentMongo(Long documentId) {
        CourseDocument doc = mysqlDocRepo.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu ID=" + documentId));

        if (doc.getMongoDocId() == null || doc.getMongoDocId().isBlank()) {
            return Optional.empty();
        }

        return mongoDocRepo.findById(doc.getMongoDocId());
    }

    /**
     * Xóa tài liệu khỏi MySQL, xóa toàn bộ Chunks và Doc trong MongoDB, xóa file vật lý
     */
    @Transactional
    public void deleteDocument(Long documentId) {
        CourseDocument doc = mysqlDocRepo.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu ID=" + documentId));

        String mongoDocId = doc.getMongoDocId();
        if (mongoDocId != null && !mongoDocId.isBlank()) {
            try {
                mongoChunkRepo.deleteByDocumentId(mongoDocId);
                mongoDocRepo.deleteById(mongoDocId);
                log.info("Đã xóa chunks và document MongoDB cho docId={}", documentId);
            } catch (Exception e) {
                log.warn("Lỗi khi xóa MongoDB doc: {}", e.getMessage());
            }
        }

        // Xóa file vật lý nếu thuộc thư mục uploads nội bộ
        if (doc.getFileUrl() != null && doc.getFileUrl().startsWith("/uploads/")) {
            try {
                String relPath = doc.getFileUrl().replace("/uploads/", "");
                Path filePath = Paths.get("d:/DATN/uploads", relPath);
                Files.deleteIfExists(filePath);
                log.info("Đã xóa file vật lý: {}", filePath);
            } catch (Exception e) {
                log.warn("Lỗi khi xóa file vật lý: {}", e.getMessage());
            }
        }

        mysqlDocRepo.delete(doc);
        log.info("Đã xóa CourseDocument MySQL ID={}", documentId);
    }

    /**
     * Bóc tách lại văn bản và tái lập chỉ mục (re-index) Chunks vào MongoDB
     */
    @Transactional
    public CourseDocumentMongo reindexDocument(Long documentId) throws Exception {
        CourseDocument doc = mysqlDocRepo.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu ID=" + documentId));

        if (doc.getFileUrl() == null || !doc.getFileUrl().startsWith("/uploads/")) {
            throw new RuntimeException("Tệp tin nằm trên CDN từ xa hoặc không có đường dẫn tệp cục bộ hợp lệ.");
        }

        String relPath = doc.getFileUrl().replace("/uploads/", "");
        Path filePath = Paths.get("d:/DATN/uploads", relPath);
        if (!Files.exists(filePath)) {
            throw new RuntimeException("Không tìm thấy tệp tin vật lý trên máy chủ: " + filePath);
        }

        byte[] bytes = Files.readAllBytes(filePath);
        String filename = filePath.getFileName().toString();
        DocumentParserService.ParsedDocumentResult parsed = documentParserService.parseFromBytes(bytes, filename);

        // Xóa chunks cũ trong MongoDB
        if (doc.getMongoDocId() != null && !doc.getMongoDocId().isBlank()) {
            mongoChunkRepo.deleteByDocumentId(doc.getMongoDocId());
        }

        // Tạo mới hoặc cập nhật Mongo doc
        CourseDocumentMongo mongoDoc;
        if (doc.getMongoDocId() != null && mongoDocRepo.existsById(doc.getMongoDocId())) {
            mongoDoc = mongoDocRepo.findById(doc.getMongoDocId()).get();
            mongoDoc.setRawContent(parsed.getFullText());
            mongoDoc.setTotalPages(parsed.getTotalPages());
            mongoDoc.setRagStatus(CourseDocumentMongo.RagStatus.PARSING);
        } else {
            mongoDoc = CourseDocumentMongo.builder()
                    .mysqlDocId(doc.getId())
                    .courseId(doc.getCourse() != null ? doc.getCourse().getId() : null)
                    .title(doc.getTitle())
                    .fileName(filename)
                    .rawContent(parsed.getFullText())
                    .totalPages(parsed.getTotalPages())
                    .ragStatus(CourseDocumentMongo.RagStatus.PARSING)
                    .build();
        }
        CourseDocumentMongo savedMongoDoc = mongoDocRepo.save(mongoDoc);
        doc.setMongoDocId(savedMongoDoc.getId());
        doc.setRagIndexed(true);
        mysqlDocRepo.save(doc);

        // Cắt đoạn và lưu vào MongoDB
        List<DocumentChunkMongo> chunksToSave = new ArrayList<>();
        int chunkGlobalIndex = 0;
        for (Map.Entry<Integer, String> entry : parsed.getPagesText().entrySet()) {
            int pageNum = entry.getKey();
            String pageText = entry.getValue();
            List<String> pageChunks = splitTextRecursive(pageText, CHUNK_SIZE, CHUNK_OVERLAP);
            for (String chunkStr : pageChunks) {
                if (chunkStr.isBlank()) continue;
                DocumentChunkMongo chunk = DocumentChunkMongo.builder()
                        .documentId(savedMongoDoc.getId())
                        .courseId(doc.getCourse() != null ? doc.getCourse().getId() : null)
                        .chunkIndex(chunkGlobalIndex++)
                        .pageNumber(pageNum)
                        .content(chunkStr)
                        .tokenCount(chunkStr.split("\\s+").length)
                        .build();
                chunksToSave.add(chunk);
            }
        }
        mongoChunkRepo.saveAll(chunksToSave);

        savedMongoDoc.setChunkCount(chunksToSave.size());
        savedMongoDoc.setRagStatus(CourseDocumentMongo.RagStatus.INDEXED);
        mongoDocRepo.save(savedMongoDoc);

        log.info("Tái lập chỉ mục RAG thành công cho docId={}: {} trang, {} chunks",
                documentId, parsed.getTotalPages(), chunksToSave.size());
        return savedMongoDoc;
    }
}
