package me.sogom.bridge.domain.fcm.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.sogom.bridge.domain.fcm.dto.message.FcmMessageDTO;
import me.sogom.bridge.domain.fcm.dto.req.FcmReqDTO;
import me.sogom.bridge.domain.fcm.service.FcmService;
import me.sogom.bridge.domain.member.code.MemberSuccessCode;
import me.sogom.bridge.global.apiPayload.ApiResponse;
import me.sogom.bridge.global.security.entity.AuthMember;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/fcm")
@Tag(name = "FCM API")
public class FcmController {

    private final FcmService fcmService;

    @Operation(
            summary = "FCM 토큰 저장",
            description = "사용자의 FCM 토큰 저장 및 갱신 API"
    )
    @PostMapping("/token")
    public ApiResponse<String> saveFcmToken(
            @AuthenticationPrincipal AuthMember authMember,
            @RequestBody @Valid FcmReqDTO.SaveFcmTokenRequest request
    ) {

        fcmService.saveFcmToken(
                authMember,
                request.fcmToken()
        );

        return ApiResponse.onSuccess(
                MemberSuccessCode.FCM_TOKEN_SAVE_SUCCESS,
                "FCM 토큰 저장 완료"
        );
    }

    @Operation(
            summary = "FCM 테스트 푸시",
            description = "테스트용 푸시 알림 발송 API"
    )
    @PostMapping("/test")
    public ApiResponse<String> testPush(
            @AuthenticationPrincipal AuthMember authMember
    ) {

        FcmMessageDTO message = FcmMessageDTO.builder()
                .title("테스트 알림")
                .body("FCM 테스트 알림입니다.")
                .type("TEST")
                .targetId(0L)
                .build();

        fcmService.sendPush(
                authMember.getMember().getId(),
                authMember.getRole(),
                message
        );

        return ApiResponse.onSuccess(
                MemberSuccessCode.FCM_TOKEN_SAVE_SUCCESS,
                "테스트 푸시 발송 완료"
        );
    }
}