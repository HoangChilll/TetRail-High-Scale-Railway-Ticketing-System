package com.hoang.ddd.application.service.notification;

import com.hoang.ddd.domain.notification.NotificationChannel;
import com.hoang.ddd.domain.notification.NotificationMessage;
import com.hoang.ddd.domain.notification.NotificationSender;
import com.hoang.ddd.domain.notification.NotificationSenderFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class NotificationService {

    // Bảng tra cứu Factory theo Kênh: Không cần if-else!
    private final Map<NotificationChannel, NotificationSenderFactory> factoryMap;

    public NotificationService(List<NotificationSenderFactory> factories) {
        // Tự động map từng Factory với Channel tương ứng
        this.factoryMap = factories.stream()
                .collect(Collectors.toMap(
                        NotificationSenderFactory::getChannel,
                        Function.identity()));
        log.info("[NotificationService] Đã nạp thành công {} factories: {}", factoryMap.size(), factoryMap.keySet());
    }

    /**
     * Gửi thông báo đến kênh mong muốn
     */
    public void send(NotificationChannel channel, NotificationMessage message) {
        NotificationSenderFactory factory = factoryMap.get(channel);

        if (factory == null) {
            log.error("Không tìm thấy Factory cho kênh thông báo: {}", channel);
            throw new IllegalArgumentException("Hệ thống chưa hỗ trợ kênh thông báo: " + channel);
        }

        // Áp dụng Factory Method: Khởi tạo Sender tương ứng
        NotificationSender sender = factory.createSender();

        // Thực thi gửi
        sender.send(message);
    }
}
