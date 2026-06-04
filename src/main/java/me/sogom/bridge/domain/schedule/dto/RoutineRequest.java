//학원 일정 등록 요청 DTO
package me.sogom.bridge.domain.schedule.dto;
import lombok.Getter;
import java.time.DayOfWeek;
import java.time.LocalTime;

@Getter
public class RoutineRequest {
    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
}