package me.sogom.bridge.domain.notification.dto.res;

import lombok.Builder;
import me.sogom.bridge.domain.notification.entity.Notification;
import me.sogom.bridge.domain.notification.entity.NotificationType;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class NotificationResDTO {

    @Builder
    public record NotificationResponse(

            Long notificationId,

            String title,

            String content,

            Boolean isRead,

            NotificationType notificationType,

            LocalDateTime createdAt,

            Long childId,

            Long missionId,

            Long performanceId,

            String deeplink,

            Map<String, Object> payload

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
                    .childId(notification.getChildId())
                    .missionId(notification.getMissionId())
                    .performanceId(notification.getPerformanceId())
                    .deeplink(notification.getTargetRoute())
                    .payload(payloadOf(notification))
                    .build();
        }

        private static Map<String, Object> payloadOf(Notification notification) {
            Map<String, Object> payload = new LinkedHashMap<>();
            if (notification.getChildId() != null) {
                payload.put("childId", notification.getChildId().toString());
                payload.put("childrenId", notification.getChildId().toString());
            }
            if (notification.getMissionId() != null) {
                payload.put("missionId", notification.getMissionId().toString());
            }
            if (notification.getPerformanceId() != null) {
                payload.put("performanceId", notification.getPerformanceId().toString());
            }
            if (notification.getTargetRoute() != null && !notification.getTargetRoute().isBlank()) {
                payload.put("targetRoute", notification.getTargetRoute());
                payload.put("deeplink", notification.getTargetRoute());
            }
            return payload;
        }
    }
}
