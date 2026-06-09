package me.sogom.bridge.domain.notification.service;

import me.sogom.bridge.domain.fcm.dto.message.FcmMessageDTO;
import me.sogom.bridge.domain.fcm.service.FcmService;
import me.sogom.bridge.domain.notification.entity.Notification;
import me.sogom.bridge.domain.notification.entity.NotificationType;
import me.sogom.bridge.domain.notification.repository.NotificationRepository;
import me.sogom.bridge.global.security.entity.MemberRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private FcmService fcmService;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void createNotificationSendsFcmPayloadWithSavedNotificationId() {
        doAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            ReflectionTestUtils.setField(notification, "id", 10L);
            return notification;
        }).when(notificationRepository).save(any(Notification.class));

        notificationService.createNotification(
                1L,
                MemberRole.PARENT,
                "미션 확인 요청",
                "자녀가 미션 확인을 요청했습니다.",
                NotificationType.MISSION_REQUESTED,
                2L,
                3L,
                4L,
                "/today-mission?childrenId=2"
        );

        ArgumentCaptor<FcmMessageDTO> messageCaptor =
                ArgumentCaptor.forClass(FcmMessageDTO.class);
        verify(fcmService).sendPush(
                eq(1L),
                eq(MemberRole.PARENT),
                messageCaptor.capture()
        );

        assertThat(messageCaptor.getValue().dataPayload())
                .containsEntry("notificationId", "10")
                .containsEntry("notificationType", "MISSION_REQUESTED")
                .containsEntry("childId", "2")
                .containsEntry("childrenId", "2")
                .containsEntry("missionId", "3")
                .containsEntry("performanceId", "4")
                .containsEntry("targetRoute", "/today-mission?childrenId=2")
                .containsEntry("deeplink", "/today-mission?childrenId=2");
    }

    @Test
    void createNotificationKeepsInboxRowWhenFcmFails() {
        doAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            ReflectionTestUtils.setField(notification, "id", 10L);
            return notification;
        }).when(notificationRepository).save(any(Notification.class));
        doThrow(new IllegalStateException("FCM down"))
                .when(fcmService)
                .sendPush(any(), any(), any(FcmMessageDTO.class));

        notificationService.createNotification(
                1L,
                MemberRole.PARENT,
                "미션 확인 요청",
                "자녀가 미션 확인을 요청했습니다.",
                NotificationType.MISSION_REQUESTED,
                2L,
                3L,
                4L,
                "/today-mission?childrenId=2"
        );

        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void readNotificationAllowsOnlyOwningMemberAndRole() {
        Notification notification = notification(1L, MemberRole.PARENT);
        when(notificationRepository.findById(10L)).thenReturn(Optional.of(notification));

        notificationService.readNotification(10L, 1L, MemberRole.PARENT);

        assertThat(notification.getIsRead()).isTrue();
    }

    @Test
    void readNotificationRejectsOtherMemberOrRole() {
        Notification notification = notification(1L, MemberRole.PARENT);
        when(notificationRepository.findById(10L)).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.readNotification(10L, 2L, MemberRole.PARENT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("본인의 알림");

        assertThatThrownBy(() -> notificationService.readNotification(10L, 1L, MemberRole.CHILDREN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("본인의 알림");
    }

    @Test
    void deleteNotificationAllowsOnlyOwningMemberAndRole() {
        Notification notification = notification(1L, MemberRole.PARENT);
        when(notificationRepository.findById(10L)).thenReturn(Optional.of(notification));

        notificationService.deleteNotification(10L, 1L, MemberRole.PARENT);

        verify(notificationRepository).delete(notification);
    }

    @Test
    void deleteNotificationRejectsOtherMemberOrRole() {
        Notification notification = notification(1L, MemberRole.PARENT);
        when(notificationRepository.findById(10L)).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.deleteNotification(10L, 2L, MemberRole.PARENT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("본인의 알림");

        assertThatThrownBy(() -> notificationService.deleteNotification(10L, 1L, MemberRole.CHILDREN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("본인의 알림");

        verify(notificationRepository, never()).delete(any(Notification.class));
    }

    private Notification notification(Long memberId, MemberRole memberRole) {
        Notification notification = Notification.builder()
                .memberId(memberId)
                .memberRole(memberRole)
                .title("알림")
                .content("내용")
                .isRead(false)
                .notificationType(NotificationType.GENERAL)
                .build();
        ReflectionTestUtils.setField(notification, "id", 10L);
        return notification;
    }
}
