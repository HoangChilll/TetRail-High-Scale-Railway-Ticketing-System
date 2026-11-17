package com.hoang.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Component
public class TelegramNotificationSender implements NotificationSender {

    private final RestClient restClient;
    private final String botToken;

    public TelegramNotificationSender(@Value("${telegram.bot.token:mock-telegram-token}") String botToken) {
        this.restClient = RestClient.builder().build();
        this.botToken = botToken;
    }

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
