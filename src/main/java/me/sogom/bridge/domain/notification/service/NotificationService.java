package me.sogom.bridge.domain.notification.service;

import lombok.RequiredArgsConstructor;
import me.sogom.bridge.domain.notification.dto.res.NotificationResDTO;
import me.sogom.bridge.domain.notification.entity.Notification;
import me.sogom.bridge.domain.notification.entity.NotificationType;
import me.sogom.bridge.domain.notification.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    // 알림 생성
    @Transactional
    public void createNotification(
            Long memberId,
            String title,
            String content,
            NotificationType notificationType
    ) {

        Notification notification = Notification.builder()
                .memberId(memberId)
                .title(title)
                .content(content)
                .isRead(false)
                .notificationType(notificationType)
                .build();

        notificationRepository.save(notification);
    }

    // 회원의 알림 목록 조회
    @Transactional(readOnly = true)
    public List<NotificationResDTO.NotificationResponse> getNotifications(Long memberId) {

        return notificationRepository.findAllByMemberIdOrderByCreatedAtDesc(memberId)
                .stream()
                .map(NotificationResDTO.NotificationResponse::of)
                .toList();
    }

    // 알림 읽음 처리
    @Transactional
    public void readNotification(Long notificationId, Long memberId) {

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다."));

        // 본인의 알림만 읽음 처리 가능
        if (!notification.getMemberId().equals(memberId)) {
            throw new IllegalArgumentException("본인의 알림만 읽을 수 있습니다.");
        }

        notification.read();
    }

    // 알림 삭제
    @Transactional
    public void deleteNotification(Long notificationId, Long memberId) {

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다."));

        // 본인의 알림만 삭제 가능
        if (!notification.getMemberId().equals(memberId)) {
            throw new IllegalArgumentException("본인의 알림만 삭제할 수 있습니다.");
        }

        notificationRepository.delete(notification);
    }
}