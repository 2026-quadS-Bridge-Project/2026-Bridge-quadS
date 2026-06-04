package me.sogom.bridge.domain.notification.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import me.sogom.bridge.domain.notification.entity.NotificationType;
import me.sogom.bridge.global.security.entity.MemberRole;

public class NotificationReqDTO {

    public record CreateNotificationRequest(

            @NotNull
            Long memberId,

            @NotNull
            MemberRole memberRole,

            @NotBlank
            String title,

            @NotBlank
            String content,

            @NotNull
            NotificationType notificationType

    ) {
    }
}