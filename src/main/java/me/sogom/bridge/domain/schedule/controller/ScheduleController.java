package me.sogom.bridge.domain.schedule.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.sogom.bridge.domain.schedule.dto.DailyScheduleResponse;
import me.sogom.bridge.domain.schedule.dto.RoutineRequest;
import me.sogom.bridge.domain.schedule.dto.TimeExtensionRequest;
import me.sogom.bridge.domain.schedule.dto.WeeklyTemplateRequest;
import me.sogom.bridge.domain.schedule.entity.DailyTimeAllocation;
import me.sogom.bridge.domain.schedule.entity.WeeklyRoutine;
import me.sogom.bridge.domain.schedule.service.ScheduleService;
import me.sogom.bridge.global.apiPayload.ApiResponse;
import me.sogom.bridge.global.apiPayload.code.GeneralSuccessCode;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/children/{childId}/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;

    //URL 예시: GET /api/v1/children/1/schedules/daily?date=2026-05-16
    @GetMapping("/daily")
    public ApiResponse<DailyScheduleResponse> getDailySchedule(
            @PathVariable Long childId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {

        //서비스 비즈니스 로직 호출 (조회 혹은 자동 동적 생성)
        DailyTimeAllocation allocation = scheduleService.getOrCreateDailyAllocation(childId, date);

        //GeneralSuccessCode.OK에 응답 데이터를 저장 후 return
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, DailyScheduleResponse.from(allocation));
    }

    //URL 예시: POST /api/v1/children/1/schedules/extend
    @PostMapping("/extend")
    public ApiResponse<DailyScheduleResponse> extendDailyTime(
            @PathVariable Long childId,
            @Valid @RequestBody TimeExtensionRequest request) {

        //부모 보상 시간 차감 및 오늘 제한 시간 증가
        DailyTimeAllocation updatedAllocation = scheduleService.extendDailyTime(
                childId,
                request.getTargetDate(),
                request.getExtraMinutes()
        );

        //업데이트 완료된 데이터를 공통 규격에 맞춰 return
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, DailyScheduleResponse.from(updatedAllocation));
    }
    //[PUT] 주간 요일별 가용시간 기본 틀(템플릿) 설정/수정
    @PutMapping("/templates")
    public ApiResponse<String> updateWeeklyTemplate(
            @PathVariable Long childId,
            @RequestBody WeeklyTemplateRequest request) {
        scheduleService.updateWeeklyTemplate(childId, request);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, "요일별 기본 가용 시간이 설정되었습니다.");
    }

    //[POST] 학원 고정 일정 등록
    @PostMapping("/routines")
    public ApiResponse<String> createRoutine(
            @PathVariable Long childId,
            @RequestBody RoutineRequest request) {
        scheduleService.createRoutine(childId, request);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, "고정 일정이 성공적으로 등록되었습니다.");
    }

    //[GET] 자녀의 주간 학원/고정 일정 전체 조회 (참고용 시간표 뷰에 그려짐)
    @GetMapping("/routines")
    public ApiResponse<List<WeeklyRoutine>> getWeeklyRoutines(@PathVariable Long childId) {
        List<WeeklyRoutine> routines = scheduleService.getWeeklyRoutines(childId);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, routines);
    }

    //[DELETE] 고정 일정 삭제
    @DeleteMapping("/routines/{routineId}")
    public ApiResponse<String> deleteRoutine(
            @PathVariable Long childId,
            @PathVariable Long routineId) {
        scheduleService.deleteRoutine(routineId);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, "일정이 삭제되었습니다.");
    }
}