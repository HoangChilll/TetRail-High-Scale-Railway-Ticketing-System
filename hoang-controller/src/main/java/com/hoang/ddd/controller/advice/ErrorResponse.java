package com.hoang.ddd.controller.advice;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * ENVELOPE PATTERN: Khuôn mẫu chuẩn định dạng phản hồi lỗi của hệ thống
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // Không in ra trường null trong JSON
public class ErrorResponse {

    private int status;
    private String error;
    private String message;
    private String path;

    @Builder.Default
    private String timestamp = LocalDateTime.now().toString();

    // Chứa chi tiết lỗi theo từng trường: ví dụ { "email": "Email không hợp lệ" }
    private Map<String, String> errors;
}
