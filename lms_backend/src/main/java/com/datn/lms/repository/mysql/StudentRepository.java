package com.datn.lms.repository.mysql;

import com.datn.lms.entity.mysql.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByStudentCode(String studentCode);
    Boolean existsByStudentCode(String studentCode);
}
