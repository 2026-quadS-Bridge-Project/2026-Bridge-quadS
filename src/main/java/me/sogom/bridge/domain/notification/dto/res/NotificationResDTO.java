package me.sogom.bridge.domain.notification.dto.res;

import lombok.Builder;
import me.sogom.bridge.domain.notification.entity.Notification;
import me.sogom.bridge.domain.notification.entity.NotificationType;

import java.time.LocalDateTime;

public class NotificationResDTO {

    @Builder
    public record NotificationResponse(

            Long notificationId,

            String title,

            String content,

            Boolean isRead,

            NotificationType notificationType,

            LocalDateTime createdAt

    ) {

        // 엔티티를 응답 DTO로 변환
        public static NotificationResponse of(Notification notification) {

            return NotificationResponse.builder()
                    .notificationId(notification.getId())
                    .title(notification.getTitle())
                    .content(notification.getContent())
                    .isRead(notification.getIsRead())
                    .notificationType(notification.getNotificationType())
                    .createdAt(notification.getCreatedAt())
                    .build();
        }
    }
}