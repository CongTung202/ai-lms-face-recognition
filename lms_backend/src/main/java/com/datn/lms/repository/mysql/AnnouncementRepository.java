package com.datn.lms.repository.mysql;

import com.datn.lms.entity.mysql.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    List<Announcement> findAllByOrderByCreatedAtDesc();
    List<Announcement> findByStatusOrderByCreatedAtDesc(String status);
    List<Announcement> findByCategoryAndStatusOrderByCreatedAtDesc(String category, String status);
    long countByStatus(String status);
}

