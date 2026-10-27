package com.hoang.ddd.infrastructure.notification.factory;

import com.hoang.ddd.domain.notification.NotificationChannel;
import com.hoang.ddd.domain.notification.NotificationSender;
import com.hoang.ddd.domain.notification.NotificationSenderFactory;
import com.hoang.ddd.infrastructure.notification.sender.TelegramNotificationSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class TelegramNotificationSenderFactory implements NotificationSenderFactory {

    @Value("${telegram.bot.token:mock-telegram-token}")
    private String botToken;

    private final RestClient restClient;

    public TelegramNotificationSenderFactory() {
        this.restClient = RestClient.builder().build();
    }

    @Override
    public NotificationSender createSender() {
        // Factory Method: Khởi tạo TelegramSender với token và restClient đã chuẩn bị
        return new TelegramNotificationSender(restClient, botToken);
    }

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.TELEGRAM;
    }
}
