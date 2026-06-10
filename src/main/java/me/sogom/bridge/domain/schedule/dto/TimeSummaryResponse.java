package me.sogom.bridge.domain.schedule.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TimeSummaryResponse {
    private boolean parentPolicyExists;
    private boolean childPlanExists;
    private String todayScheduleStatus;
    private String yearMonth;
    private int basePolicyMinutes;
    private DailyScheduleResponse todaySchedule;
    private int rewardPoolMinutes;
}
