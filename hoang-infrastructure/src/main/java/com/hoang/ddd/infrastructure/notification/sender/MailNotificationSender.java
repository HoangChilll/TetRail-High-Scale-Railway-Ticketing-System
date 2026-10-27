package com.hoang.ddd.infrastructure.notification.sender;

import com.hoang.ddd.domain.notification.NotificationChannel;
import com.hoang.ddd.domain.notification.NotificationMessage;
import com.hoang.ddd.domain.notification.NotificationSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@Slf4j
@RequiredArgsConstructor
public class MailNotificationSender implements NotificationSender {

    private final JavaMailSender javaMailSender;
    private final String fromAddress;

    @Override
    public void send(NotificationMessage message) {
        log.info("[MailSender] Đang gửi email tới: {}", message.getRecipient());
        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom(fromAddress);
            mailMessage.setTo(message.getRecipient());
            mailMessage.setSubject(message.getSubject());
            mailMessage.setText(message.getContent());

            javaMailSender.send(mailMessage);
            log.info("[MailSender] Gửi email thành công tới: {}", message.getRecipient());
        } catch (Exception e) {
            log.error("[MailSender] Thất bại khi gửi email tới: {}. Lỗi: {}", message.getRecipient(), e.getMessage());
            throw new RuntimeException("Gửi email thất bại", e);
        }
    }

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.EMAIL;
    }
}
