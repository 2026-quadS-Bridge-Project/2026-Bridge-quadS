package me.sogom.bridge.domain.mission.dto.res;

import lombok.Builder;
import me.sogom.bridge.domain.mission.entity.MissionPerformance;
import me.sogom.bridge.domain.mission.entity.MissionStatus;
import me.sogom.bridge.global.storage.PhotoUrlResolver;

public class MissionPerformanceResDTO {

    @Builder
    public record MissionPerformanceResponse(
            Long performanceId,
            Long missionId,
            Long childId,
            MissionStatus status,
            String proofImageUrl
    ) {
        public static MissionPerformanceResponse of(MissionPerformance performance, PhotoUrlResolver photoUrlResolver) {
            return MissionPerformanceResponse.builder()
                    .performanceId(performance.getId())
                    .missionId(performance.getMission().getId())
                    .childId(performance.getChild().getId())
                    .status(performance.getStatus())
                    .proofImageUrl(photoUrlResolver.resolveOrNull(performance.getProofImageKey()))
                    .build();
        }
    }
}
