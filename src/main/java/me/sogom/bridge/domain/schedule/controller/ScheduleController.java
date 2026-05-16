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

// 시큐리티 및 인증 객체 임포트
import me.sogom.bridge.global.security.entity.AuthMember;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;

    //[GET] 자녀의 특정 날짜 시간표 조회
    @GetMapping("/daily")
    public ApiResponse<DailyScheduleResponse> getDailySchedule(
            @AuthenticationPrincipal AuthMember user,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {

        //자녀 엔티티를 꺼낸 후 ID를 추출
        Long childId = user.asChildren().getId();
        DailyTimeAllocation allocation = scheduleService.getOrCreateDailyAllocation(childId, date);

        //GeneralSuccessCode.OK에 응답 데이터를 저장 후 return
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, DailyScheduleResponse.from(allocation));
    }

    //[POST] 자녀의 특정 날짜 시간 연장
    @PostMapping("/extend")
    public ApiResponse<DailyScheduleResponse> extendDailyTime(
            @AuthenticationPrincipal AuthMember user,
            @Valid @RequestBody TimeExtensionRequest request) {

        //부모 보상 시간 차감 및 오늘 제한 시간 증가
        Long childId = user.asChildren().getId();
        DailyTimeAllocation updatedAllocation = scheduleService.extendDailyTime(
                childId, request.getTargetDate(), request.getExtraMinutes()
        );

        //업데이트 완료된 데이터를 공통 규격에 맞춰 return
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, DailyScheduleResponse.from(updatedAllocation));
    }

    //[POST] 당일 사용 시간 최종 정산 및 잔여 시간 보상 풀 환불 마감
    @PostMapping("/settle")
    public ApiResponse<DailyScheduleResponse> settleDailyTime(
            @AuthenticationPrincipal AuthMember user,
            @RequestParam int actualUsed,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {

        Long childId = user.asChildren().getId();
        DailyTimeAllocation settledAllocation = scheduleService.settleDailyTime(childId, date, actualUsed);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, DailyScheduleResponse.from(settledAllocation));
    }

    //[PUT] 주간 요일별 가용시간 기본 틀(템플릿) 설정 및 수정
    @PutMapping("/templates")
    public ApiResponse<String> updateWeeklyTemplate(
            @AuthenticationPrincipal AuthMember user,
            @RequestBody WeeklyTemplateRequest request) {
        Long childId = user.asChildren().getId();
        scheduleService.updateWeeklyTemplate(childId, request);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, "요일별 기본 가용 시간이 성공적으로 설정되었습니다.");
    }

    //[POST] 학원 고정 일정(Routine) 등록
    @PostMapping("/routines")
    public ApiResponse<String> createRoutine(
            @AuthenticationPrincipal AuthMember user,
            @RequestBody RoutineRequest request) {
        Long childId = user.asChildren().getId();
        scheduleService.createRoutine(childId, request);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, "고정 일정이 성공적으로 등록되었습니다.");
    }

    //[GET] 자녀의 주간 학원/고정 일정 전체 조회
    @GetMapping("/routines")
    public ApiResponse<List<WeeklyRoutine>> getWeeklyRoutines(
            @AuthenticationPrincipal AuthMember user) {

        Long childId = user.asChildren().getId();
        List<WeeklyRoutine> routines = scheduleService.getWeeklyRoutines(childId);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, routines);
    }

    //[DELETE] 학원 고정 일정 삭제
    @DeleteMapping("/routines/{routineId}")
    public ApiResponse<String> deleteRoutine(
            @AuthenticationPrincipal AuthMember user,
            @PathVariable Long routineId) {
        scheduleService.deleteRoutine(routineId);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, "일정이 정상적으로 삭제되었습니다.");
    }
}