//요일별 기본 분배 시간 설정 요청 DTO
package me.sogom.bridge.domain.schedule.dto;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.DayOfWeek;

@Getter
@NoArgsConstructor // 역직렬화를 위한 기본 생성자
public class WeeklyTemplateRequest {
    private String yearMonth;   // 예: "2026-05"
    private int weekNumber;     // 예: 1 (1주차)

    private DayOfWeek dayOfWeek;
    private int baseMinutes;
}