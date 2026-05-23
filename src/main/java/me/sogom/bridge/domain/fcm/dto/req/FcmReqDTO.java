package me.sogom.bridge.domain.fcm.dto.req;

import jakarta.validation.constraints.NotBlank;

public class FcmReqDTO {

    public record SaveFcmTokenRequest(

            @NotBlank
            String fcmToken

    ) {
    }
}