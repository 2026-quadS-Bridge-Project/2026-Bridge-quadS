package me.sogom.bridge.domain.mission.dto.res;

import lombok.Builder;
import me.sogom.bridge.domain.mission.entity.MissionPerformance;
import me.sogom.bridge.domain.mission.entity.MissionStatus;

public class MissionPerformanceResDTO {

    @Builder
    public record MissionPerformanceResponse(
            Long performanceId,
            Long missionId,
            Long childId,
            MissionStatus status,
            String proofUrl
    ) {
        public static MissionPerformanceResponse of(MissionPerformance performance) {
            return MissionPerformanceResponse.builder()
                    .performanceId(performance.getId())
                    .missionId(performance.getMission().getId())
                    .childId(performance.getChild().getId())
                    .status(performance.getStatus())
                    .proofUrl(performance.getProofUrl())
                    .build();
        }
    }
}

