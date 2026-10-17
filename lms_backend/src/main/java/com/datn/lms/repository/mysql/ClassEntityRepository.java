package com.datn.lms.repository.mysql;

import com.datn.lms.entity.mysql.ClassEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassEntityRepository extends JpaRepository<ClassEntity, Long> {
    Optional<ClassEntity> findByClassCode(String classCode);
    boolean existsByClassCode(String classCode);
    List<ClassEntity> findByMajorId(Long majorId);
    List<ClassEntity> findByFacultyId(Long facultyId);
}
