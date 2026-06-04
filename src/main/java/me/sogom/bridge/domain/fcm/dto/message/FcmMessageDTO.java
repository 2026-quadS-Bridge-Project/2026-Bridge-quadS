package me.sogom.bridge.domain.fcm.dto.message;

import lombok.Builder;

@Builder
public record FcmMessageDTO(

        String title,

        String body,

        String type,

        Long targetId

) {
}