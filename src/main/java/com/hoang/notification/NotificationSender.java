package com.hoang.notification;

public interface NotificationSender {
    void send(NotificationMessage message);

    NotificationChannel getChannel();
}
