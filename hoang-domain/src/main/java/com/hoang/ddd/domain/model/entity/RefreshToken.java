package com.hoang.ddd.domain.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Chuỗi ngẫu nhiên UUID duy nhất
    @Column(name = "token", nullable = false, unique = true, length = 100)
    private String token;

    // Liên kết với User sở hữu token
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Thời điểm hết hạn tính theo chuẩn UTC Instant
    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

    // Đánh dấu token đã bị thu hồi/vô hiệu hóa hay chưa (khi logout hoặc xoay vòng)
    @Column(name = "revoked", nullable = false)
    @Builder.Default
    private boolean revoked = false;

    /**
     * Phương thức kiểm tra nghiệp vụ: Token đã hết hạn chưa?
     */
    public boolean isExpired() {
        return Instant.now().isAfter(this.expiryDate);
    }
}
