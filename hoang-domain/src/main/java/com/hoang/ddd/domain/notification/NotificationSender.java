package com.hoang.ddd.domain.notification;

public interface NotificationSender {
    void send(NotificationMessage message);

    NotificationChannel getChannel();
}
