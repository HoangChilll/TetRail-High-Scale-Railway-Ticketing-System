package com.hoang.ddd.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    // Secret Key 256-bit dạng Base64 hợp lệ theo application.yaml
    private static final String VALID_SECRET_BASE64 =
            "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long EXPIRATION_MS = 3600000L; // 1 giờ

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", VALID_SECRET_BASE64);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationMs", EXPIRATION_MS);
        jwtTokenProvider.init();
    }

    private Authentication createMockAuthentication(String username, List<String> roles) {
        var authorities = roles.stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
        return new UsernamePasswordAuthenticationToken(username, null, authorities);
    }

    @Test
    @DisplayName("Sinh Token thành công: Token chứa đúng username, role và chữ ký hợp lệ")
    void generateToken_Success() {
        // ARRANGE
        Authentication auth = createMockAuthentication("hoang_dev", List.of("ROLE_USER", "ROLE_ADMIN"));

        // ACT
        String token = jwtTokenProvider.generateToken(auth);

        // ASSERT
        assertNotNull(token);
        assertFalse(token.isBlank());
        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals("hoang_dev", jwtTokenProvider.getUsernameFromToken(token));

        List<String> roles = jwtTokenProvider.getRolesFromToken(token);
        assertNotNull(roles);
        assertEquals(2, roles.size());
        assertTrue(roles.contains("ROLE_USER"));
        assertTrue(roles.contains("ROLE_ADMIN"));
    }

    @Test
    @DisplayName("Trích xuất username từ Token chính xác")
    void getUsernameFromToken_Success() {
        Authentication auth = createMockAuthentication("passenger_01", List.of("ROLE_USER"));
        String token = jwtTokenProvider.generateToken(auth);

        String username = jwtTokenProvider.getUsernameFromToken(token);
        assertEquals("passenger_01", username);
    }

    @Test
    @DisplayName("Trích xuất danh sách Roles từ Token chính xác")
    void getRolesFromToken_Success() {
        Authentication auth = createMockAuthentication("super_admin", List.of("ROLE_ADMIN"));
        String token = jwtTokenProvider.generateToken(auth);

        List<String> roles = jwtTokenProvider.getRolesFromToken(token);
        assertEquals(1, roles.size());
        assertEquals("ROLE_ADMIN", roles.get(0));
    }

    @Test
    @DisplayName("validateToken trả về false khi Token đã hết hạn")
    void validateToken_Fails_WhenTokenExpired() {
        // ARRANGE: Tạo JwtTokenProvider với thời gian sống âm (đã hết hạn)
        JwtTokenProvider expiredProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(expiredProvider, "jwtSecret", VALID_SECRET_BASE64);
        ReflectionTestUtils.setField(expiredProvider, "jwtExpirationMs", -10000L); // Hết hạn 10s trước
        expiredProvider.init();

        Authentication auth = createMockAuthentication("hoang_dev", List.of("ROLE_USER"));
        String expiredToken = expiredProvider.generateToken(auth);

        // ACT & ASSERT: Kiểm tra validate với provider chính
        boolean isValid = jwtTokenProvider.validateToken(expiredToken);
        assertFalse(isValid, "Token hết hạn phải trả về false");
    }

    @Test
    @DisplayName("validateToken trả về false khi chữ ký bị chỉnh sửa (Tampered token)")
    void validateToken_Fails_WhenSignatureTampered() {
        // ARRANGE: Sinh token hợp lệ rồi làm sai lệch chữ ký
        Authentication auth = createMockAuthentication("hoang_dev", List.of("ROLE_USER"));
        String validToken = jwtTokenProvider.generateToken(auth);

        // Thay đổi 3 ký tự cuối cùng của chuỗi signature
        String tamperedToken = validToken.substring(0, validToken.length() - 3) + "xyz";

        // ACT & ASSERT
        assertFalse(jwtTokenProvider.validateToken(tamperedToken), "Token bị sửa chữ ký phải trả về false");
    }

    @Test
    @DisplayName("validateToken trả về false khi Token được ký bằng Secret Key khác")
    void validateToken_Fails_WhenSignedByDifferentKey() {
        // ARRANGE: Provider khác dùng secret key khác
        String differentSecretBase64 = "5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437";
        JwtTokenProvider otherProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(otherProvider, "jwtSecret", differentSecretBase64);
        ReflectionTestUtils.setField(otherProvider, "jwtExpirationMs", EXPIRATION_MS);
        otherProvider.init();

        Authentication auth = createMockAuthentication("attacker", List.of("ROLE_USER"));
        String tokenFromOtherKey = otherProvider.generateToken(auth);

        // ACT & ASSERT: Provider hiện tại kiểm tra token ký bằng key khác
        assertFalse(jwtTokenProvider.validateToken(tokenFromOtherKey), "Token ký bởi key khác phải trả về false");
    }

    @Test
    @DisplayName("validateToken trả về false khi Token sai định dạng (Malformed)")
    void validateToken_Fails_WhenMalformedToken() {
        assertFalse(jwtTokenProvider.validateToken("not.a.valid.jwt.string"));
        assertFalse(jwtTokenProvider.validateToken("header.payload")); // Thiếu signature
        assertFalse(jwtTokenProvider.validateToken("invalidTokenWithoutDots"));
    }

    @Test
    @DisplayName("validateToken trả về false khi Token rỗng hoặc null")
    void validateToken_Fails_WhenEmptyOrNull() {
        assertFalse(jwtTokenProvider.validateToken(""));
        assertFalse(jwtTokenProvider.validateToken("   "));
        assertFalse(jwtTokenProvider.validateToken(null));
    }
}

