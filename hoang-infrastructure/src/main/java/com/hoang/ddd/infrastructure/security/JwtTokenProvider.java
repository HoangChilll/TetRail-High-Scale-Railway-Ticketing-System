package com.hoang.ddd.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        // Giải mã chuỗi Base64 từ application.yaml thành SecretKey chuẩn HMAC-SHA
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * BUILDER PATTERN: Sinh Access Token từ thông tin Authentication đã xác thực
     * thành công
     */
    public String generateToken(Authentication authentication) {
        String username = authentication.getName();

        // Lấy danh sách Role/Authority để đóng gói vào Claim của token
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(username)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey, Jwts.SIG.HS256) // STRATEGY PATTERN: Thuật toán ký HS256
                .compact();
    }

    /**
     * Trích xuất username (Subject) từ token
     */
    public String getUsernameFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Trích xuất danh sách Roles từ Custom Claim
     */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        return parseClaims(token).get("roles", List.class);
    }

    /**
     * Xác thực tính toàn vẹn và hạn sử dụng của Token
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // Bao gồm: SignatureException (chữ ký sai), ExpiredJwtException (hết hạn),
            // MalformedJwtException (bị sửa đổi)
            return false;
        }
    }

    /**
     * Giải mã và kiểm tra chữ ký token sử dụng JJWT 0.12.6 API
     */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
