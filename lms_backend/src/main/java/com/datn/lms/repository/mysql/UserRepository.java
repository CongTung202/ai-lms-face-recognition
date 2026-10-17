package com.datn.lms.repository.mysql;

import com.datn.lms.entity.mysql.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    java.util.List<User> findByRole(User.Role role);
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);
}
