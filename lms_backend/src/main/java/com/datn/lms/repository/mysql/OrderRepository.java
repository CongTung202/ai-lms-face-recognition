package com.datn.lms.repository.mysql;

import com.datn.lms.entity.mysql.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderCode(String orderCode);
    List<Order> findAllByOrderByCreatedAtDesc();
    List<Order> findByPaymentStatusOrderByCreatedAtDesc(Order.PaymentStatus status);
    List<Order> findByStudentUserIdOrderByCreatedAtDesc(Long studentUserId);
    long countByPaymentStatus(Order.PaymentStatus status);
}
