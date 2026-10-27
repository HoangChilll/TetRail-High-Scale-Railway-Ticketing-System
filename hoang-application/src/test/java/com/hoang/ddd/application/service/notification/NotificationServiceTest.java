package com.hoang.ddd.application.service.notification;

import com.hoang.ddd.domain.notification.NotificationChannel;
import com.hoang.ddd.domain.notification.NotificationMessage;
import com.hoang.ddd.domain.notification.NotificationSender;
import com.hoang.ddd.domain.notification.NotificationSenderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationSenderFactory mailFactory;

    @Mock
    private NotificationSender mailSender;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        // Giả lập mailFactory nhận diện kênh EMAIL và tạo ra mailSender
        when(mailFactory.getChannel()).thenReturn(NotificationChannel.EMAIL);

        // Khởi tạo Service với danh sách các Factory
        notificationService = new NotificationService(List.of(mailFactory));
    }

    @Test
    @DisplayName("Gửi qua EMAIL thành công: Factory tạo đúng MailSender và gọi send()")
    void send_Email_Success() {
        when(mailFactory.createSender()).thenReturn(mailSender);

        NotificationMessage message = NotificationMessage.builder()
                .recipient("test@tetrail.vn")
                .subject("Test")
                .content("Nội dung")
                .build();

        // ACT
        notificationService.send(NotificationChannel.EMAIL, message);

        // ASSERT: Xác minh Factory Method hoạt động chuẩn
        verify(mailFactory, times(1)).createSender();
        verify(mailSender, times(1)).send(message);
    }

    @Test
    @DisplayName("Ném ngoại lệ khi kênh chưa có Factory hỗ trợ")
    void send_ThrowsException_WhenChannelNotSupported() {
        NotificationMessage message = NotificationMessage.builder()
                .recipient("123456789")
                .content("Cảnh báo")
                .build();

        // TELEGRAM chưa được đăng ký trong setUp()
        assertThrows(IllegalArgumentException.class,
                () -> notificationService.send(NotificationChannel.TELEGRAM, message));
    }
}
