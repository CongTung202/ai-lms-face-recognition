package com.datn.lms.entity.mongodb;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * MongoDB Document lưu trữ toàn bộ nội dung trích xuất của tài liệu môn học
 * phục vụ cho việc cắt đoạn (chunking) và nhúng vector (embedding) cho RAG.
 */
@Document(collection = "course_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseDocumentMongo {

    @Id
    private String id; // MongoDB ObjectId (dạng chuỗi hex 24 ký tự)

    @Indexed
    @Field("mysql_doc_id")
    private Long mysqlDocId; // ID tham chiếu từ bảng MySQL course_documents

    @Indexed
    @Field("course_id")
    private Long courseId; // Khóa học tương ứng

    @Field("title")
    private String title;

    @Field("file_name")
    private String fileName;

    @Field("raw_content")
    private String rawContent; // Toàn bộ văn bản đã OCR hoặc bóc tách từ PDF/DOCX

    @Field("summary")
    private String summary; // Tóm tắt ngắn gọn do AI sinh ra

    @Field("total_pages")
    private Integer totalPages;

    @Field("chunk_count")
    @Builder.Default
    private Integer chunkCount = 0; // Số lượng đoạn văn bản (chunks) đã tách

    @Field("rag_status")
    @Builder.Default
    private RagStatus ragStatus = RagStatus.PENDING;

    @Field("metadata")
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;

    public enum RagStatus {
        PENDING,     // Mới tải lên, chưa xử lý
        PARSING,     // Đang bóc tách nội dung
        CHUNKED,     // Đã cắt xong các đoạn văn bản
        INDEXED,     // Đã nhúng vector và sẵn sàng cho RAG
        FAILED       // Xử lý thất bại
    }
}
