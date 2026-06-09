package me.sogom.bridge.domain.fcm.dto.message;

import lombok.Builder;

import java.util.LinkedHashMap;
import java.util.Map;

@Builder
public record FcmMessageDTO(

        String title,

        String body,

        String type,

        Long targetId,

        Long childId,

        Long missionId,

        Long performanceId,

        String deeplink

) {

    public Map<String, String> dataPayload() {
        Map<String, String> payload = new LinkedHashMap<>();
        putIfPresent(payload, "title", title);
        putIfPresent(payload, "body", body);
        putIfPresent(payload, "type", type);
        putIfPresent(payload, "notificationType", type);
        putIfPresent(payload, "targetId", targetId);
        putIfPresent(payload, "notificationId", targetId);
        putIfPresent(payload, "childId", childId);
        putIfPresent(payload, "childrenId", childId);
        putIfPresent(payload, "missionId", missionId);
        putIfPresent(payload, "performanceId", performanceId);
        putIfPresent(payload, "deeplink", deeplink);
        putIfPresent(payload, "targetRoute", deeplink);
        return payload;
    }

    private static void putIfPresent(Map<String, String> payload, String key, Object value) {
        if (value == null) {
            return;
        }
        String stringValue = value.toString();
        if (!stringValue.isBlank()) {
            payload.put(key, stringValue);
        }
    }
}
