package com.hoang.ddd.domain.notification;

import lombok.Builder;
import lombok.Getter;
import java.util.Map;

@Getter
@Builder
public class NotificationMessage {
    private final String recipient; // Email hoặc Telegram chat_id
    private final String subject; // Tiêu đề (dành cho Email)
    private final String content; // Nội dung tin nhắn
    private final Map<String, Object> metadata; // Thông tin bổ sung
}
