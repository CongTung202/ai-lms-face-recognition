package com.datn.lms.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassDto {
    private Long id;
    private String classCode;
    private String name;
    private String className;
    private String academicYear;
    private String homeroomTeacher;
    private Long majorId;
    private String majorCode;
    private String majorName;
    private Long facultyId;
    private String facultyCode;
    private String facultyName;
    private Integer studentsCount;
    private Integer totalStudents;
    private LocalDateTime createdAt;
}
