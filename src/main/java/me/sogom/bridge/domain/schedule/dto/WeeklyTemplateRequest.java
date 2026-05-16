//요일별 기본 분배 시간 설정 요청 DTO
package me.sogom.bridge.domain.schedule.dto;
import lombok.Getter;
import java.time.DayOfWeek;

@Getter
public class WeeklyTemplateRequest {
    private DayOfWeek dayOfWeek;
    private int baseMinutes;
}