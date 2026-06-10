package me.sogom.bridge.domain.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.sogom.bridge.domain.fcm.dto.message.FcmMessageDTO;
import me.sogom.bridge.domain.fcm.service.FcmService;
import me.sogom.bridge.domain.notification.dto.res.NotificationResDTO;
import me.sogom.bridge.domain.notification.entity.Notification;
import me.sogom.bridge.domain.notification.entity.NotificationType;
import me.sogom.bridge.domain.notification.repository.NotificationRepository;
import me.sogom.bridge.global.security.entity.MemberRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    // FCM 서비스
    private final FcmService fcmService;

    // 알림 생성
    @Transactional
    public void createNotification(
            Long memberId,
            MemberRole memberRole,
            String title,
            String content,
            NotificationType notificationType
    ) {
        createNotification(
                memberId,
                memberRole,
                title,
                content,
                notificationType,
                null,
                null,
                null,
                null
        );
    }

    @Transactional
    public void createNotification(
            Long memberId,
            MemberRole memberRole,
            String title,
            String content,
            NotificationType notificationType,
            Long childId,
            Long missionId,
            Long performanceId,
            String targetRoute
    ) {

        Notification notification = Notification.builder()
                .memberId(memberId)
                .memberRole(memberRole)
                .title(title)
                .content(content)
                .isRead(false)
                .notificationType(notificationType)
                .childId(childId)
                .missionId(missionId)
                .performanceId(performanceId)
                .targetRoute(targetRoute)
                .build();

        // 알림 저장
        notificationRepository.save(notification);

        // FCM 푸시 전송
        try {

            FcmMessageDTO message = FcmMessageDTO.builder()
                    .title(title)
                    .body(content)
                    .type(notificationType.name())
                    .targetId(notification.getId())
                    .childId(childId)
                    .missionId(missionId)
                    .performanceId(performanceId)
                    .deeplink(targetRoute)
                    .build();

            fcmService.sendPush(
                    memberId,
                    memberRole,
                    message
            );

        } catch (Exception e) {

            // FCM 실패 시 Notification 저장은 유지
            log.error("FCM 푸시 알림 전송 실패", e);
        }
    }

    // 회원의 알림 목록 조회
    @Transactional(readOnly = true)
    public List<NotificationResDTO.NotificationResponse> getNotifications(
            Long memberId,
            MemberRole memberRole
    ) {

        return notificationRepository
                .findAllByMemberIdAndMemberRoleOrderByCreatedAtDesc(
                        memberId,
                        memberRole
                )
                .stream()
                .map(NotificationResDTO.NotificationResponse::of)
                .toList();
    }

    // 알림 읽음 처리
    @Transactional
    public void readNotification(
            Long notificationId,
            Long memberId,
            MemberRole memberRole
    ) {

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다."));

        // 본인의 알림만 읽음 처리 가능
        if (!notification.getMemberId().equals(memberId)
                || !notification.getMemberRole().equals(memberRole)) {

            throw new IllegalArgumentException("본인의 알림만 읽을 수 있습니다.");
        }

        notification.read();
    }

    // 알림 삭제
    @Transactional
    public void deleteNotification(
            Long notificationId,
            Long memberId,
            MemberRole memberRole
    ) {

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다."));

        // 본인의 알림만 삭제 가능
        if (!notification.getMemberId().equals(memberId)
                || !notification.getMemberRole().equals(memberRole)) {

            throw new IllegalArgumentException("본인의 알림만 삭제할 수 있습니다.");
        }

        notificationRepository.delete(notification);
    }
}
