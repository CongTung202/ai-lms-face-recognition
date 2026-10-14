package com.datn.lms.security;

import com.datn.lms.entity.mysql.Student;
import com.datn.lms.entity.mysql.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@Slf4j
public class JwtTokenProvider {

    private final SecretKey key;
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}") String secret,
            @Value("${jwt.expiration-ms:86400000}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /**
     * Tạo Token đăng nhập chính thức (Hiệu lực 24 giờ)
     */
    public String generateToken(User user, Student student) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        var builder = Jwts.builder()
                .subject(user.getUsername())
                .claim("userId", user.getId())
                .claim("fullName", user.getFullName())
                .claim("role", user.getRole().name())
                .issuedAt(now)
                .expiration(expiryDate);

        if (student != null) {
            builder.claim("studentCode", student.getStudentCode())
                   .claim("faceRegistered", student.getFaceRegistered())
                   .claim("faceImageUrl", student.getFaceImageUrl());
        }

        return builder.signWith(key).compact();
    }

    /**
     * Tạo Token tạm thời (Hiệu lực 10 phút) phục vụ quy trình xác thực / đăng ký khuôn mặt
     */
    public String generateTempToken(User user, String purpose) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + (10 * 60 * 1000)); // 10 phút

        return Jwts.builder()
                .subject(user.getUsername())
                .claim("userId", user.getId())
                .claim("purpose", purpose)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("JWT Token không hợp lệ: {}", e.getMessage());
            return false;
        }
    }

    public Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("userId", Long.class);
    }

    public String getUsernameFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }

    public String getPurposeFromToken(String token) {
        return getClaimsFromToken(token).get("purpose", String.class);
    }
}
