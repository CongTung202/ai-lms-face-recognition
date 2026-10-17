package com.datn.lms.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacultyDto {
    private Long id;
    private String facultyCode;
    private String name;
    private String facultyName;
    private String description;
    private String majors;
    private java.util.List<String> majorList;
    private Integer majorsCount;
    private Integer classesCount;
    private LocalDateTime createdAt;
}
