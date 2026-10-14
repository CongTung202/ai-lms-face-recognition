package com.datn.lms.entity.mongodb;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Phân đoạn văn bản (Text Chunks) kèm Vector Embeddings (768D / 1536D)
 * Dùng cho tính năng AI RAG: Sinh viên hỏi bài -> Tìm chunks tương đồng -> Trả lời theo giáo trình
 */
@Document(collection = "document_chunks")
@CompoundIndex(name = "idx_doc_chunk", def = "{'document_id': 1, 'chunk_index': 1}")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentChunkMongo {

    @Id
    private String id;

    @Indexed
    @Field("document_id")
    private String documentId; // Trỏ về CourseDocumentMongo.id

    @Indexed
    @Field("course_id")
    private Long courseId; // Thuộc khóa học nào

    @Field("chunk_index")
    private Integer chunkIndex; // Thứ tự đoạn trong tài liệu (0, 1, 2, ...)

    @Field("page_number")
    private Integer pageNumber; // Nằm ở trang số mấy trong file PDF

    @Field("content")
    private String content; // Nội dung đoạn trích (khoảng 500 - 1000 ký tự)

    @Field("token_count")
    private Integer tokenCount; // Ước tính số token

    @Field("embedding")
    private List<Double> embedding; // Vector đặc trưng ngữ nghĩa (Dense vector do FastAPI / OpenAI / HuggingFace tạo ra)

    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;
}
