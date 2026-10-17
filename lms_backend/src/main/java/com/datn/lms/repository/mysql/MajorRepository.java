package com.datn.lms.repository.mysql;

import com.datn.lms.entity.mysql.Major;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MajorRepository extends JpaRepository<Major, Long> {
    Optional<Major> findByMajorCode(String majorCode);
    List<Major> findByFacultyId(Long facultyId);
    long countByFacultyId(Long facultyId);
}
