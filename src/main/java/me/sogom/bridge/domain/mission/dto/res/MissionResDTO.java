package me.sogom.bridge.domain.mission.dto.res;

import lombok.Builder;
import me.sogom.bridge.domain.mission.entity.Mission;
import me.sogom.bridge.domain.mission.entity.MissionCategory;
import me.sogom.bridge.domain.mission.entity.MissionSetting;
import me.sogom.bridge.domain.mission.entity.ResetCycle;
import me.sogom.bridge.domain.mission.entity.VerificationType;

public class MissionResDTO {

    @Builder
    public record CreateMissionResponse(
            Long missionId,
            Long childId,
            String title,
            MissionCategory category,
            ResetCycle resetCycle,
            VerificationType verificationType,
            int reward,
            String description
    ) {
        public static CreateMissionResponse of(Mission mission, MissionSetting setting) {
            return CreateMissionResponse.builder()
                    .missionId(mission.getId())
                    .childId(mission.getChild().getId())
                    .title(mission.getTitle())
                    .category(setting.getCategory())
                    .resetCycle(setting.getResetCycle())
                    .verificationType(setting.getVerificationType())
                    .reward(setting.getReward())
                    .description(setting.getDescription())
                    .build();
        }
    }
}
