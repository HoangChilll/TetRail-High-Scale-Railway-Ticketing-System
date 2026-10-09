package com.hoang.ddd.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * TEMPLATE METHOD PATTERN: Kế thừa OncePerRequestFilter đảm bảo chạy đúng 1 lần
 * / request.
 * INTERCEPTOR / CHAIN OF RESPONSIBILITY PATTERN: Đón đầu request để xác thực
 * Token.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, UserDetailsService userDetailsService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // 1. Trích xuất chuỗi JWT từ header "Authorization"
        String jwt = getJwtFromRequest(request);

        // 2. Kiểm tra token có tồn tại và hợp lệ không
        if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
            // 3. Trích xuất username từ Token
            String username = jwtTokenProvider.getUsernameFromToken(jwt);

            // 4. Nạp UserDetails từ Database thông qua UserDetailsService đã viết ở Bước 4
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (userDetails != null && userDetails.isEnabled()) {
                // 5. Tạo đối tượng Authentication đã chứng thực hợp lệ
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());

                // Gắn thêm các chi tiết phụ trợ của request (như IP, Session ID nếu có)
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 6. CONTEXT PATTERN: Nạp danh tính người dùng vào ThreadLocal của request hiện
                // tại
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        // 7. Chuyển tiếp request cho filter tiếp theo trong chuỗi (bắt buộc)
        filterChain.doFilter(request, response);
    }

    /**
     * Bóc tách tiền tố "Bearer " từ Header Authorization
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // Cắt bỏ 7 ký tự đầu "Bearer "
        }
        return null;
    }
}
