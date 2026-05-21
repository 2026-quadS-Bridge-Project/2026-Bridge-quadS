package me.sogom.bridge.domain.notification.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import me.sogom.bridge.domain.notification.entity.NotificationType;

public class NotificationReqDTO {

    public record CreateNotificationRequest(

            @NotNull
            Long memberId,

            @NotBlank
            String title,

            @NotBlank
            String content,

            @NotNull
            NotificationType notificationType

    ) {
    }
}