package com.hoang.ddd.infrastructure.notification.sender;

import com.hoang.ddd.domain.notification.NotificationChannel;
import com.hoang.ddd.domain.notification.NotificationMessage;
import com.hoang.ddd.domain.notification.NotificationSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class TelegramNotificationSender implements NotificationSender {

    private final RestClient restClient;
    private final String botToken;

    @Override
    public void send(NotificationMessage message) {
        log.info("[TelegramSender] Đang gửi tin nhắn Telegram tới Chat ID: {}", message.getRecipient());
        try {
            String url = String.format("https://api.telegram.org/bot%s/sendMessage", botToken);

            // Payload Telegram yêu cầu: chat_id và text
            Map<String, String> body = Map.of(
                    "chat_id", message.getRecipient(),
                    "text",
                    (message.getSubject() != null ? "[" + message.getSubject() + "]\n" : "") + message.getContent());

            restClient.post()
                    .uri(url)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            log.info("[TelegramSender] Gửi tin nhắn Telegram thành công tới Chat ID: {}", message.getRecipient());
        } catch (Exception e) {
            log.error("[TelegramSender] Thất bại khi gửi Telegram tới {}. Lỗi: {}", message.getRecipient(),
                    e.getMessage());
            throw new RuntimeException("Gửi tin Telegram thất bại", e);
        }
    }

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.TELEGRAM;
    }
}
