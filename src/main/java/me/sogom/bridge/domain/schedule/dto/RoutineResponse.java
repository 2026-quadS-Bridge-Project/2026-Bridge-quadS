package me.sogom.bridge.domain.schedule.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import me.sogom.bridge.domain.schedule.entity.WeeklyRoutine;
import java.time.DayOfWeek;
import java.time.LocalTime;

@Getter
@AllArgsConstructor
@Builder
public class RoutineResponse {
    private Long id;
    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;

    // 엔티티를 안전하고 깔끔한 DTO로 변환해주는 메서드
    public static RoutineResponse from(WeeklyRoutine routine) {
        return RoutineResponse.builder()
                .id(routine.getId())
                .dayOfWeek(routine.getDayOfWeek())
                .startTime(routine.getStartTime())
                .endTime(routine.getEndTime())
                .build();
    }
}