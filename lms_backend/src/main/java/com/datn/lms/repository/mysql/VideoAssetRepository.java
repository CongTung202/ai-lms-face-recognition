package com.datn.lms.repository.mysql;

import com.datn.lms.entity.mysql.VideoAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VideoAssetRepository extends JpaRepository<VideoAsset, Long> {
    Optional<VideoAsset> findByLessonId(Long lessonId);
}
