package com.likelion.tometa.domain.user.repository;

import com.likelion.tometa.domain.user.entity.UserNotificationSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface UserNotificationSettingRepository
        extends JpaRepository<UserNotificationSetting, Long> {

    boolean existsByUser_Id(Long userId);

    Optional<UserNotificationSetting> findByUser_Id(Long userId);

    @Query("""
            select setting.user.id
            from UserNotificationSetting setting
            where setting.recordReminderEnabled = true
              and setting.recordReminderTime = :reminderTime
            order by setting.user.id
            """)
    List<Long> findRecordReminderTargetUserIds(
            @Param("reminderTime") LocalTime reminderTime
    );
}
