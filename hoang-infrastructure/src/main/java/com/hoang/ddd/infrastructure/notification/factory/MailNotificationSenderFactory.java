package com.hoang.ddd.infrastructure.notification.factory;

import com.hoang.ddd.domain.notification.NotificationChannel;
import com.hoang.ddd.domain.notification.NotificationSender;
import com.hoang.ddd.domain.notification.NotificationSenderFactory;
import com.hoang.ddd.infrastructure.notification.sender.MailNotificationSender;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MailNotificationSenderFactory implements NotificationSenderFactory {

    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username:noreply@tetrail.com}")
    private String fromEmail;

    @Override
    public NotificationSender createSender() {
        // Factory Method: Quyết định cách thức khởi tạo sản phẩm cụ thể
        return new MailNotificationSender(javaMailSender, fromEmail);
    }

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.EMAIL;
    }
}
