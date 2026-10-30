package com.hoang.ddd.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = mock(FilterChain.class);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Request có Bearer Token hợp lệ: Xác thực thành công và nạp Authentication vào SecurityContext")
    void doFilterInternal_ValidToken_AuthenticatesAndSetsSecurityContext() throws ServletException, IOException {
        // ARRANGE
        String validJwt = "valid.jwt.token.123";
        request.addHeader("Authorization", "Bearer " + validJwt);

        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.isEnabled()).thenReturn(true);
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_USER"))).when(userDetails).getAuthorities();

        when(jwtTokenProvider.validateToken(validJwt)).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken(validJwt)).thenReturn("hoang_dev");
        when(userDetailsService.loadUserByUsername("hoang_dev")).thenReturn(userDetails);

        // ACT
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // ASSERT
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication, "Authentication trong SecurityContext không được null");
        assertEquals(userDetails, authentication.getPrincipal());
        assertTrue(authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_USER")));

        // Phải chuyển tiếp request tới Filter tiếp theo trong chuỗi
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Request không có Header Authorization: Bỏ qua và không gán Authentication")
    void doFilterInternal_NoAuthorizationHeader_ProceedsWithoutAuthentication() throws ServletException, IOException {
        // ACT
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // ASSERT
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtTokenProvider, never()).validateToken(anyString());
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Request có Header Authorization không bắt đầu bằng 'Bearer ': Bỏ qua")
    void doFilterInternal_NonBearerHeader_ProceedsWithoutAuthentication() throws ServletException, IOException {
        // ARRANGE
        request.addHeader("Authorization", "Basic dXNlcjpwYXNzd29yZA==");

        // ACT
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // ASSERT
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtTokenProvider, never()).validateToken(anyString());
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Request có Bearer Token không hợp lệ hoặc đã hết hạn: Không gán Authentication")
    void doFilterInternal_InvalidOrExpiredToken_ProceedsWithoutAuthentication() throws ServletException, IOException {
        // ARRANGE
        String invalidJwt = "expired.or.invalid.token";
        request.addHeader("Authorization", "Bearer " + invalidJwt);
        when(jwtTokenProvider.validateToken(invalidJwt)).thenReturn(false);

        // ACT
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // ASSERT
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtTokenProvider, times(1)).validateToken(invalidJwt);
        verify(jwtTokenProvider, never()).getUsernameFromToken(anyString());
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Token hợp lệ nhưng UserDetails trả về bị vô hiệu hóa (disabled): Không gán Authentication")
    void doFilterInternal_UserDisabled_DoesNotSetAuthentication() throws ServletException, IOException {
        // ARRANGE
        String validJwt = "valid.jwt.token";
        request.addHeader("Authorization", "Bearer " + validJwt);

        UserDetails disabledUser = mock(UserDetails.class);
        when(disabledUser.isEnabled()).thenReturn(false);

        when(jwtTokenProvider.validateToken(validJwt)).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken(validJwt)).thenReturn("disabled_user");
        when(userDetailsService.loadUserByUsername("disabled_user")).thenReturn(disabledUser);

        // ACT
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // ASSERT
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Token hợp lệ nhưng không tìm thấy UserDetails (null): Không gán Authentication")
    void doFilterInternal_UserDetailsNull_DoesNotSetAuthentication() throws ServletException, IOException {
        // ARRANGE
        String validJwt = "valid.jwt.token";
        request.addHeader("Authorization", "Bearer " + validJwt);

        when(jwtTokenProvider.validateToken(validJwt)).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken(validJwt)).thenReturn("deleted_user");
        when(userDetailsService.loadUserByUsername("deleted_user")).thenReturn(null);

        // ACT
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // ASSERT
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, times(1)).doFilter(request, response);
    }
}

