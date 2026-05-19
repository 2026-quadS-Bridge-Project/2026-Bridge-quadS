package me.sogom.bridge.domain.schedule.dto;

import lombok.Getter;

@Getter
public class WeeklyBudgetRequest {
    private int weekNumber;
    private int allocatedMinutes;
}