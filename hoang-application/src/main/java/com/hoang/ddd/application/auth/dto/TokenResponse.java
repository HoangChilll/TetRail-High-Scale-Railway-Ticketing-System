package com.hoang.ddd.application.auth.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenResponse {

    private String accessToken;

    @Builder.Default
    private String tokenType = "Bearer";

    // Thời gian sống tính theo giây (chuẩn OAuth2 / OIDC RFC 6749)
    private long expiresIn;
}
