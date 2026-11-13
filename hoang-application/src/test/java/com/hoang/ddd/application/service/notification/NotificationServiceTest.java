package com.hoang.ddd.application.service.notification;

import com.hoang.ddd.domain.notification.NotificationChannel;
import com.hoang.ddd.domain.notification.NotificationMessage;
import com.hoang.ddd.domain.notification.NotificationSender;
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

    @Test
    void rejectsDuplicateChannels() {
        NotificationSender secondSender = mock(NotificationSender.class);
        when(secondSender.getChannel()).thenReturn(NotificationChannel.EMAIL);

        assertThrows(IllegalStateException.class,
                () -> new NotificationService(List.of(mailSender, secondSender)));
    }

    @Test
    void propagatesSenderFailure() {
        NotificationMessage message = NotificationMessage.builder().content("Test").build();
        doThrow(new IllegalStateException("Send failed")).when(mailSender).send(message);

        assertThrows(IllegalStateException.class,
                () -> notificationService.send(NotificationChannel.EMAIL, message));
    }

    @Mock
    private NotificationSender mailSender;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        when(mailSender.getChannel()).thenReturn(NotificationChannel.EMAIL);

        notificationService = new NotificationService(List.of(mailSender));
    }

    @Test
    @DisplayName("Gửi qua EMAIL bằng MailSender")
    void send_Email_Success() {

        NotificationMessage message = NotificationMessage.builder()
                .recipient("test@tetrail.vn")
                .subject("Test")
                .content("Nội dung")
                .build();

        // ACT
        notificationService.send(NotificationChannel.EMAIL, message);

        verify(mailSender, times(1)).send(message);
    }

    @Test
    @DisplayName("Ném ngoại lệ khi kênh chưa có Sender hỗ trợ")
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
