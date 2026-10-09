package com.hoang.ddd.infrastructure.security.annotation;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * META-ANNOTATION PATTERN:
 * Bọc @AuthenticationPrincipal của Spring Security để Controller có thể trích
 * xuất
 * danh tính người dùng hiện tại một cách tường minh và an toàn.
 */
@Target({ ElementType.PARAMETER, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AuthenticationPrincipal
public @interface CurrentUser {
}
