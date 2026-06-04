package me.sogom.bridge.domain.mission.entity;

import java.time.LocalDateTime;

public enum ResetCycle {
    DAILY,
    WEEKLY,
    MONTHLY;

    public LocalDateTime nextResetAfter(LocalDateTime from) {
        return switch (this) {
            case DAILY -> from.plusDays(1);
            case WEEKLY -> from.plusWeeks(1);
            case MONTHLY -> from.plusMonths(1);
        };
    }
}
