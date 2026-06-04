package me.sogom.bridge.domain.mission.dto.res;

import me.sogom.bridge.domain.mission.entity.MissionCategory;

public record MissionSummaryResponse(
        Long missionId,
        String title,
        MissionCategory category,
        int reward
) {}
