package me.sogom.bridge.domain.schedule.dto;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class TimeExtensionRequest {
    private LocalDate targetDate;

    @Min(value = 1, message = "연장할 시간은 1분 이상이어야 합니다.")
    private int extraMinutes;
}