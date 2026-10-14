package com.datn.lms.service;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@Slf4j
public class DocumentParserService {

    @Getter
    @Builder
    public static class ParsedDocumentResult {
        private String fileName;
        private String fullText;
        private int totalPages;
        private Map<Integer, String> pagesText; // Số trang -> nội dung text trang đó
    }

    /**
     * Bóc tách toàn bộ nội dung từ các tệp tài liệu giáo trình (.pdf, .docx, .txt)
     */
    public ParsedDocumentResult parseDocument(MultipartFile file) throws Exception {
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "document.txt";
        log.info("Bắt đầu bóc tách văn bản từ tệp: {} ({} KB)", filename, file.getSize() / 1024);

        if (filename.endsWith(".pdf")) {
            return parsePdf(file);
        } else if (filename.endsWith(".docx")) {
            return parseDocx(file);
        } else if (filename.endsWith(".txt")) {
            return parseTxt(file);
        } else {
            throw new IllegalArgumentException("Định dạng tệp không được hỗ trợ. Chỉ hỗ trợ .pdf, .docx, .txt");
        }
    }

    private ParsedDocumentResult parsePdf(MultipartFile file) throws Exception {
        Map<Integer, String> pages = new LinkedHashMap<>();
        StringBuilder fullTextBuilder = new StringBuilder();

        try (InputStream is = file.getInputStream();
             PDDocument document = Loader.loadPDF(is.readAllBytes())) {

            int totalPages = document.getNumberOfPages();
            PDFTextStripper stripper = new PDFTextStripper();

            for (int p = 1; p <= totalPages; p++) {
                stripper.setStartPage(p);
                stripper.setEndPage(p);
                String pageText = stripper.getText(document).trim();

                if (!pageText.isEmpty()) {
                    pages.put(p, pageText);
                    fullTextBuilder.append("\n--- [Trang ").append(p).append("] ---\n");
                    fullTextBuilder.append(pageText).append("\n");
                }
            }

            return ParsedDocumentResult.builder()
                    .fileName(file.getOriginalFilename())
                    .fullText(fullTextBuilder.toString().trim())
                    .totalPages(totalPages)
                    .pagesText(pages)
                    .build();
        }
    }

    private ParsedDocumentResult parseDocx(MultipartFile file) throws Exception {
        Map<Integer, String> pages = new LinkedHashMap<>();
        StringBuilder fullTextBuilder = new StringBuilder();

        try (InputStream is = file.getInputStream();
             XWPFDocument docx = new XWPFDocument(is)) {

            List<XWPFParagraph> paragraphs = docx.getParagraphs();
            int estimatedPage = 1;
            int wordCount = 0;
            StringBuilder currentPageBuilder = new StringBuilder();

            for (XWPFParagraph p : paragraphs) {
                String text = p.getText().trim();
                if (!text.isEmpty()) {
                    currentPageBuilder.append(text).append("\n");
                    fullTextBuilder.append(text).append("\n");

                    wordCount += text.split("\\s+").length;
                    if (wordCount > 350) { // Mỗi trang Word chuẩn khoảng 350-400 từ
                        pages.put(estimatedPage++, currentPageBuilder.toString().trim());
                        currentPageBuilder.setLength(0);
                        wordCount = 0;
                    }
                }
            }

            if (currentPageBuilder.length() > 0) {
                pages.put(estimatedPage, currentPageBuilder.toString().trim());
            }

            return ParsedDocumentResult.builder()
                    .fileName(file.getOriginalFilename())
                    .fullText(fullTextBuilder.toString().trim())
                    .totalPages(Math.max(1, pages.size()))
                    .pagesText(pages)
                    .build();
        }
    }

    private ParsedDocumentResult parseTxt(MultipartFile file) throws Exception {
        String content = new String(file.getBytes(), StandardCharsets.UTF_8).trim();
        Map<Integer, String> pages = Map.of(1, content);

        return ParsedDocumentResult.builder()
                .fileName(file.getOriginalFilename())
                .fullText(content)
                .totalPages(1)
                .pagesText(pages)
                .build();
    }
}
