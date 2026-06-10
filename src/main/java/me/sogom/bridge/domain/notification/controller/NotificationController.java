package me.sogom.bridge.domain.notification.controller;

import lombok.RequiredArgsConstructor;
import me.sogom.bridge.domain.notification.dto.req.NotificationReqDTO;
import me.sogom.bridge.domain.notification.dto.res.NotificationResDTO;
import me.sogom.bridge.domain.notification.service.NotificationService;
import me.sogom.bridge.global.apiPayload.ApiResponse;
import me.sogom.bridge.global.apiPayload.code.GeneralSuccessCode;
import me.sogom.bridge.global.security.entity.AuthMember;
import me.sogom.bridge.global.security.entity.MemberRole;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    // 알림 생성 API
    @PostMapping
    public ApiResponse<String> createNotification(
            @RequestBody NotificationReqDTO.CreateNotificationRequest request
    ) {

        notificationService.createNotification(
                request.memberId(),
                request.memberRole(),
                request.title(),
                request.content(),
                request.notificationType(),
                request.childId(),
                request.missionId(),
                request.performanceId(),
                request.targetRoute()
        );

        return ApiResponse.onSuccess(
                GeneralSuccessCode.OK,
                "알림 생성 완료"
        );
    }

    // 알림 목록 조회 API
    @GetMapping
    public ApiResponse<List<NotificationResDTO.NotificationResponse>> getNotifications(
            @AuthenticationPrincipal AuthMember authMember
    ) {

        // 현재 로그인한 회원 ID 조회
        Long memberId = authMember.getMember().getId();

        // 현재 로그인한 회원 역할 조회
        MemberRole memberRole = authMember.getRole();

        return ApiResponse.onSuccess(
                GeneralSuccessCode.OK,
                notificationService.getNotifications(memberId, memberRole)
        );
    }

    // 알림 읽음 처리 API
    @PatchMapping("/{notificationId}/read")
    public ApiResponse<String> readNotification(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal AuthMember authMember
    ) {

        // 현재 로그인한 회원 ID 조회
        Long memberId = authMember.getMember().getId();

        // 현재 로그인한 회원 역할 조회
        MemberRole memberRole = authMember.getRole();

        notificationService.readNotification(
                notificationId,
                memberId,
                memberRole
        );

        return ApiResponse.onSuccess(
                GeneralSuccessCode.OK,
                "알림 읽음 처리 완료"
        );
    }

    // 알림 삭제 API
    @DeleteMapping("/{notificationId}")
    public ApiResponse<String> deleteNotification(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal AuthMember authMember
    ) {

        // 현재 로그인한 회원 ID 조회
        Long memberId = authMember.getMember().getId();

        // 현재 로그인한 회원 역할 조회
        MemberRole memberRole = authMember.getRole();

        notificationService.deleteNotification(
                notificationId,
                memberId,
                memberRole
        );

        return ApiResponse.onSuccess(
                GeneralSuccessCode.OK,
                "알림 삭제 완료"
        );
    }
}
