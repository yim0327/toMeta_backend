package com.likelion.tometa.domain.user.scheduler;

import com.likelion.tometa.domain.record.repository.DailyRecordRepository;
import com.likelion.tometa.domain.user.repository.UserNotificationSettingRepository;
import com.likelion.tometa.domain.user.service.PushNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecordReminderSchedulerTest {

    private static final Instant MONDAY_15_40_KST =
            Instant.parse("2026-08-24T06:40:00Z");

    @Mock
    private UserNotificationSettingRepository
            userNotificationSettingRepository;

    @Mock
    private DailyRecordRepository dailyRecordRepository;

    @Mock
    private PushNotificationService pushNotificationService;

    private RecordReminderScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new RecordReminderScheduler(
                userNotificationSettingRepository,
                dailyRecordRepository,
                pushNotificationService,
                Clock.fixed(
                        MONDAY_15_40_KST,
                        ZoneId.of("Asia/Seoul")
                )
        );
    }

    @Test
    void scheduler_sendsReminderToUserWhoHasNotWrittenRecord() {
        LocalDate recordDate =
                LocalDate.of(2026, 8, 24);
        LocalTime reminderTime =
                LocalTime.of(15, 40);

        when(
                userNotificationSettingRepository
                        .findRecordReminderTargetUserIds(
                                reminderTime
                        )
        ).thenReturn(List.of(1L));

        when(
                dailyRecordRepository
                        .existsByUser_IdAndRecordDate(
                                1L,
                                recordDate
                        )
        ).thenReturn(false);

        when(
                pushNotificationService
                        .sendRecordReminder(
                                1L,
                                recordDate
                        )
        ).thenReturn(1);

        scheduler.sendRecordReminders();

        verify(
                userNotificationSettingRepository
        ).findRecordReminderTargetUserIds(
                reminderTime
        );

        verify(
                pushNotificationService
        ).sendRecordReminder(
                1L,
                recordDate
        );
    }

    @Test
    void scheduler_skipsUserWhoAlreadyWroteRecord() {
        LocalDate recordDate =
                LocalDate.of(2026, 8, 24);
        LocalTime reminderTime =
                LocalTime.of(15, 40);

        when(
                userNotificationSettingRepository
                        .findRecordReminderTargetUserIds(
                                reminderTime
                        )
        ).thenReturn(List.of(1L));

        when(
                dailyRecordRepository
                        .existsByUser_IdAndRecordDate(
                                1L,
                                recordDate
                        )
        ).thenReturn(true);

        scheduler.sendRecordReminders();

        verify(
                pushNotificationService,
                never()
        ).sendRecordReminder(
                1L,
                recordDate
        );
    }

    @Test
    void scheduler_continuesWithNextUserAfterNotificationFailure() {
        LocalDate recordDate =
                LocalDate.of(2026, 8, 24);
        LocalTime reminderTime =
                LocalTime.of(15, 40);

        when(
                userNotificationSettingRepository
                        .findRecordReminderTargetUserIds(
                                reminderTime
                        )
        ).thenReturn(
                List.of(
                        1L,
                        2L
                )
        );

        when(
                dailyRecordRepository
                        .existsByUser_IdAndRecordDate(
                                1L,
                                recordDate
                        )
        ).thenReturn(false);

        when(
                dailyRecordRepository
                        .existsByUser_IdAndRecordDate(
                                2L,
                                recordDate
                        )
        ).thenReturn(false);

        when(
                pushNotificationService
                        .sendRecordReminder(
                                1L,
                                recordDate
                        )
        ).thenThrow(
                new RuntimeException(
                        "notification failed"
                )
        );

        when(
                pushNotificationService
                        .sendRecordReminder(
                                2L,
                                recordDate
                        )
        ).thenReturn(1);

        assertDoesNotThrow(
                scheduler::sendRecordReminders
        );

        verify(
                pushNotificationService
        ).sendRecordReminder(
                1L,
                recordDate
        );

        verify(
                pushNotificationService
        ).sendRecordReminder(
                2L,
                recordDate
        );
    }
}