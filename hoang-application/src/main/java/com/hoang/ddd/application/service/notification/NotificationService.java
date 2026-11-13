package com.hoang.ddd.application.service.notification;

import com.hoang.ddd.domain.notification.NotificationChannel;
import com.hoang.ddd.domain.notification.NotificationMessage;
import com.hoang.ddd.domain.notification.NotificationSender;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    private final Map<NotificationChannel, NotificationSender> senders = new HashMap<>();

    public NotificationService(List<NotificationSender> notificationSenders) {
        for (NotificationSender sender : notificationSenders) {
            NotificationChannel channel = sender.getChannel();
            if (senders.containsKey(channel)) {
                throw new IllegalStateException("Trùng kênh thông báo: " + channel);
            }
            senders.put(channel, sender);
        }
    }

    public void send(NotificationChannel channel, NotificationMessage message) {
        NotificationSender sender = senders.get(channel);
        if (sender == null) {
            throw new IllegalArgumentException("Hệ thống chưa hỗ trợ kênh thông báo: " + channel);
        }
        sender.send(message);
    }
}
