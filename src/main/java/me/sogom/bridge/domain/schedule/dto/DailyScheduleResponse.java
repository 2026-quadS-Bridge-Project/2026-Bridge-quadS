package me.sogom.bridge.domain.schedule.dto;

import lombok.Builder;
import lombok.Getter;
import me.sogom.bridge.domain.schedule.entity.DailyTimeAllocation;

import java.time.LocalDate;

@Getter
@Builder
public class DailyScheduleResponse {
    private Long id;
    private LocalDate targetDate;
    private int baseMinutes;
    private int extendedMinutes;
    private int totalAvailableMinutes;

    // 엔티티를 DTO로 변환
    public static DailyScheduleResponse from(DailyTimeAllocation allocation) {
        return DailyScheduleResponse.builder()
                .id(allocation.getId())
                .targetDate(allocation.getTargetDate())
                .baseMinutes(allocation.getBaseMinutes())
                .extendedMinutes(allocation.getExtendedMinutes())
                .totalAvailableMinutes(allocation.getTotalAvailableTime())
                .build();
    }

    public static DailyScheduleResponse preview(LocalDate targetDate, int baseMinutes, int extendedMinutes) {
        return DailyScheduleResponse.builder()
                .id(null)
                .targetDate(targetDate)
                .baseMinutes(baseMinutes)
                .extendedMinutes(extendedMinutes)
                .totalAvailableMinutes(baseMinutes + extendedMinutes)
                .build();
    }
}
