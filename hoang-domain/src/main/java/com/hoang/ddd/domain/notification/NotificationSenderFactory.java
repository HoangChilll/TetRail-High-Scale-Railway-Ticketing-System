package com.hoang.ddd.domain.notification;

public interface NotificationSenderFactory {
    NotificationSender createSender();

    // Kênh thông báo mà Factory này chịu trách nhiệm khởi tạo
    NotificationChannel getChannel();
}
