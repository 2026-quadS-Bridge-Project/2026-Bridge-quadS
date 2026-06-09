package me.sogom.bridge.domain.notification.dto.res;

import me.sogom.bridge.domain.notification.entity.Notification;
import me.sogom.bridge.domain.notification.entity.NotificationType;
import me.sogom.bridge.global.security.entity.MemberRole;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationResponseTest {

    @Test
    void includesRoutingPayload() {
        Notification notification = Notification.builder()
                .memberId(1L)
                .memberRole(MemberRole.PARENT)
                .title("미션 확인 요청")
                .content("자녀가 미션 확인을 요청했습니다.")
                .isRead(false)
                .notificationType(NotificationType.MISSION_REQUESTED)
                .childId(2L)
                .missionId(3L)
                .performanceId(4L)
                .targetRoute("/today-mission?childrenId=2")
                .build();
        ReflectionTestUtils.setField(notification, "id", 5L);

        NotificationResDTO.NotificationResponse response =
                NotificationResDTO.NotificationResponse.of(notification);

        assertThat(response.childId()).isEqualTo(2L);
        assertThat(response.missionId()).isEqualTo(3L);
        assertThat(response.performanceId()).isEqualTo(4L);
        assertThat(response.deeplink()).isEqualTo("/today-mission?childrenId=2");
        assertThat(response.payload())
                .containsEntry("notificationId", "5")
                .containsEntry("notificationType", "MISSION_REQUESTED")
                .containsEntry("type", "MISSION_REQUESTED")
                .containsEntry("childId", "2")
                .containsEntry("childrenId", "2")
                .containsEntry("missionId", "3")
                .containsEntry("performanceId", "4")
                .containsEntry("targetRoute", "/today-mission?childrenId=2")
                .containsEntry("deeplink", "/today-mission?childrenId=2");
    }
}
