package me.sogom.bridge.domain.fcm.dto.message;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FcmMessageDTOTest {

    @Test
    void dataPayloadIncludesNotificationAndRoutingAliases() {
        FcmMessageDTO message = FcmMessageDTO.builder()
                .title("미션 확인 요청")
                .body("자녀가 미션 확인을 요청했습니다.")
                .type("MISSION_REQUESTED")
                .targetId(10L)
                .childId(2L)
                .missionId(3L)
                .performanceId(4L)
                .deeplink("/today-mission?childrenId=2")
                .build();

        Map<String, String> payload = message.dataPayload();

        assertThat(payload)
                .containsEntry("title", "미션 확인 요청")
                .containsEntry("body", "자녀가 미션 확인을 요청했습니다.")
                .containsEntry("type", "MISSION_REQUESTED")
                .containsEntry("notificationType", "MISSION_REQUESTED")
                .containsEntry("targetId", "10")
                .containsEntry("notificationId", "10")
                .containsEntry("childId", "2")
                .containsEntry("childrenId", "2")
                .containsEntry("missionId", "3")
                .containsEntry("performanceId", "4")
                .containsEntry("deeplink", "/today-mission?childrenId=2")
                .containsEntry("targetRoute", "/today-mission?childrenId=2");
    }

    @Test
    void dataPayloadOmitsNullAndBlankFields() {
        FcmMessageDTO message = FcmMessageDTO.builder()
                .title(" ")
                .type("GENERAL")
                .targetId(10L)
                .build();

        assertThat(message.dataPayload())
                .containsEntry("type", "GENERAL")
                .containsEntry("notificationType", "GENERAL")
                .containsEntry("targetId", "10")
                .containsEntry("notificationId", "10")
                .doesNotContainKeys("title", "body", "childId", "childrenId", "deeplink", "targetRoute");
    }
}
