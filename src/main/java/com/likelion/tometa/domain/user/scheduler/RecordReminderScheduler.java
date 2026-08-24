package com.likelion.tometa.domain.user.scheduler;

import com.likelion.tometa.domain.record.repository.DailyRecordRepository;
import com.likelion.tometa.domain.user.repository.UserNotificationSettingRepository;
import com.likelion.tometa.domain.user.service.PushNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecordReminderScheduler {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    private final UserNotificationSettingRepository userNotificationSettingRepository;
    private final DailyRecordRepository dailyRecordRepository;
    private final PushNotificationService pushNotificationService;
    private final Clock clock;

    @Scheduled(
            cron = "${app.notification.scheduler.record-reminder-cron:0 * * * * *}",
            zone = "Asia/Seoul"
    )
    public void sendRecordReminders() {
        LocalDateTime now = currentDateTime().truncatedTo(ChronoUnit.MINUTES);
        LocalDate recordDate = now.toLocalDate();
        LocalTime reminderTime = now.toLocalTime();

        List<Long> targetUserIds = userNotificationSettingRepository
                .findRecordReminderTargetUserIds(reminderTime);

        int skipped = 0;
        int notified = 0;
        int failed = 0;

        for (Long userId : targetUserIds) {
            try {
                boolean alreadyRecorded =
                        dailyRecordRepository.existsByUser_IdAndRecordDate(userId, recordDate);

                if (alreadyRecorded) {
                    skipped++;
                    continue;
                }

                notified += pushNotificationService.sendRecordReminder(userId, recordDate);
            } catch (RuntimeException e) {
                failed++;

                log.atWarn()
                        .setCause(e)
                        .addArgument(userId)
                        .addArgument(recordDate)
                        .addArgument(reminderTime)
                        .log(
                                "Record reminder notification failed. " +
                                        "userId={}, recordDate={}, reminderTime={}"
                        );
            }
        }

        log.info(
                "Record reminder scheduler completed. " +
                        "recordDate={}, reminderTime={}, targets={}, " +
                        "skipped={}, notifications={}, failed={}",
                recordDate,
                reminderTime,
                targetUserIds.size(),
                skipped,
                notified,
                failed
        );
    }

    private LocalDateTime currentDateTime() {
        return LocalDateTime.now(clock.withZone(KOREA_ZONE));
    }
}
