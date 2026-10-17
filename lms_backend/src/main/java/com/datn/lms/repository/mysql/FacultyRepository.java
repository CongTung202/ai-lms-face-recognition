package com.datn.lms.repository.mysql;

import com.datn.lms.entity.mysql.Faculty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FacultyRepository extends JpaRepository<Faculty, Long> {
    Optional<Faculty> findByFacultyCode(String facultyCode);
    boolean existsByFacultyCode(String facultyCode);
}
