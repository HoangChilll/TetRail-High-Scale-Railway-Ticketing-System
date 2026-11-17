package com.hoang;

import com.hoang.config.SchedulingConfig;

import com.hoang.scheduler.BookingExpirationScheduler;
import com.hoang.repository.BookingRepository;
import com.hoang.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SchedulingConfigTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(SchedulingConfig.class)
            .withBean(BookingRepository.class, () -> mock(BookingRepository.class))
            .withBean(TicketRepository.class, () -> mock(TicketRepository.class))
            .withBean(Clock.class, Clock::systemUTC)
            .withBean(BookingExpirationScheduler.class);

    @Test
    void registersExpirationTaskByDefault() {
        runner.run(context -> assertThat(context.getBean(ScheduledAnnotationBeanPostProcessor.class)
                .getScheduledTasks()).hasSize(1));
    }

    @Test
    void allowsDisablingAutomaticExecutionForTests() {
        runner.withPropertyValues("app.scheduling.enabled=false")
                .run(context -> assertThat(context.getBeansOfType(ScheduledAnnotationBeanPostProcessor.class))
                        .isEmpty());
    }
}
